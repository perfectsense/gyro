package gyro.core.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodePrinter;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.PrinterContext;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;

public class State {

    private final FileBackend backend;
    private final boolean test;
    private final Set<String> states = new HashSet<>();
    private final Set<String> diffFiles;
    private final Map<String, String> swapNames = new HashMap<>();
    private final Map<String, String> swapKeys = new HashMap<>();
    private final RootScope root;

    public State(RootScope current, RootScope pending, boolean test, Set<String> diffFiles) throws Exception {
        this.backend = current.getBackend();
        this.test = test;
        this.diffFiles = diffFiles != null ? ImmutableSet.copyOf(diffFiles) : null;

        this.root = new RootScope(current.getFile(), backend, null, current.getLoadFiles());

        root.load();

        for (FileScope state : root.getFileScopes()) {
            states.add(state.getFile());
        }

        for (FileScope state : pending.getFileScopes()) {
            states.add(state.getFile());
        }
    }

    public boolean isTest() {
        return test;
    }

    public Set<String> getDiffFiles() {
        return diffFiles;
    }

    public void update(Change change) {
        if (change instanceof Replace) {
            return;
        }

        Diffable diffable = change.getDiffable();

        if (!(diffable instanceof Resource)) {
            return;
        }

        Resource resource = (Resource) diffable;
        boolean typeRoot = DiffableType.getInstance(resource.getClass()).isRoot();

        // Delete goes through every state to remove the resource.
        if (change instanceof Delete) {
            if (typeRoot) {
                String key = resource.primaryKey();
                root.removeResource(swapKeys.getOrDefault(key, key));

            } else {
                root.findResources()
                    .stream()
                    .filter(r -> r.equals(resource.parentResource()))
                    .forEach(r -> updateSubresource(r, resource, true));
            }

        } else {
            if (typeRoot) {
                String key = resource.primaryKey();
                root.addResource(swapKeys.getOrDefault(key, key), resource);

            } else {
                String key = resource.parentResource().primaryKey();
                updateSubresource(root.findResource(swapKeys.getOrDefault(key, key)), resource, false);
            }
        }

        save();
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        DiffableField field = DiffableType.getInstance(parent.getClass()).getField(subresource.name());
        Object value = field.getValue(parent);

        if (value instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> collection = (Collection<Object>) value;

            if (delete) {
                collection.removeIf(subresource::equals);

            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                boolean found = false;

                for (ListIterator<Object> i = list.listIterator(); i.hasNext();) {
                    Object item = i.next();

                    if (subresource.equals(item)) {
                        i.set(subresource);
                        found = true;
                    }
                }

                if (!found) {
                    list.add(subresource);
                }

            } else {
                collection.removeIf(subresource::equals);
                collection.add(subresource);
            }

        } else if (value instanceof Resource) {
            field.setValue(parent, delete ? null : subresource);
        }
    }

    private void save() {
        NodePrinter printer = new NodePrinter();

        for (String file : states) {

            try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    backend.openOutput(file),
                    StandardCharsets.UTF_8))) {

                PrinterContext context = new PrinterContext(out, 0);

                for (Resource resource : root.findResourcesIn(Collections.singleton(file))) {
                    printer.visit(
                        new ResourceNode(
                            DiffableType.getInstance(resource.getClass()).getName(),
                            new ValueNode(swapNames.getOrDefault(resource.primaryKey(), resource.name())),
                            toBodyNodes(resource)),
                        context);
                }

            } catch (IOException error) {
                throw new GyroException(error.getMessage());
            }
        }
    }

    private List<Node> toBodyNodes(Diffable diffable) {
        List<Node> body = new ArrayList<>();
        Set<String> configuredFields = diffable.configuredFields;

        if (configuredFields != null && !configuredFields.isEmpty()) {
            body.add(toPairNode("_configured-fields", configuredFields));
        }

        for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
            Object value = field.getValue(diffable);

            if (value == null) {
                continue;
            }

            String key = field.getName();

            if (value instanceof Boolean
                || value instanceof Map
                || value instanceof Number
                || value instanceof String) {

                body.add(toPairNode(key, value));

            } else if (value instanceof Date) {
                body.add(toPairNode(key, value.toString()));

            } else if (value instanceof Diffable) {
                if (field.shouldBeDiffed()) {
                    body.add(new KeyBlockNode(key, null, toBodyNodes((Diffable) value)));

                } else {
                    body.add(toPairNode(key, value));
                }

            } else if (value instanceof Collection) {
                if (field.shouldBeDiffed()) {
                    for (Object item : (Collection<?>) value) {
                        body.add(new KeyBlockNode(key, null, toBodyNodes((Diffable) item)));
                    }

                } else {
                    body.add(toPairNode(key, value));
                }

            } else {
                throw new UnsupportedOperationException(String.format(
                        "Can't convert an instance of [%s] into a node!",
                        value.getClass().getName()));
            }
        }

        return body;
    }

    private PairNode toPairNode(Object key, Object value) {
        return new PairNode(toNode(key), toNode(value));
    }

    private Node toNode(Object value) {
        if (value instanceof Boolean
            || value instanceof Number
            || value instanceof String) {

            return new ValueNode(value);

        } else if (value instanceof Collection) {
            List<Node> items = new ArrayList<>();

            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    items.add(toNode(item));
                }
            }

            return new ListNode(items);

        } else if (value instanceof Map) {
            List<PairNode> entries = new ArrayList<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object v = entry.getValue();

                if (v != null) {
                    entries.add(toPairNode(entry.getKey(), v));
                }
            }

            return new MapNode(entries);

        } else if (value instanceof Resource) {
            Resource resource = (Resource) value;
            DiffableType type = DiffableType.getInstance(resource.getClass());

            if (resource.external) {
                return new ValueNode(type.getIdField().getValue(resource));

            } else {
                return new ReferenceNode(
                    Arrays.asList(
                        new ValueNode(type.getName()),
                        new ValueNode(swapNames.getOrDefault(resource.primaryKey(), resource.name()))),
                    Collections.emptyList());
            }

        } else {
            throw new UnsupportedOperationException(String.format(
                    "Can't convert an instance of [%s] into a node!",
                    value.getClass().getName()));
        }
    }

    public void swap(Resource x, Resource y) {
        String xType = DiffableType.getInstance(x.getClass()).getName();
        String yType = DiffableType.getInstance(y.getClass()).getName();

        if (!Objects.equals(xType, yType)) {
            throw new GyroException(String.format(
                "Can't swap resources that have different types! [%s] [%s]",
                xType,
                yType));
        }

        String xKey = x.primaryKey();
        String yKey = y.primaryKey();

        swapNames.put(xKey, y.name());
        swapNames.put(yKey, x.name());
        swapKeys.put(xKey, yKey);
        swapKeys.put(yKey, xKey);
        save();
    }

}
