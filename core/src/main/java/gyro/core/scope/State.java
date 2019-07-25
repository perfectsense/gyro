package gyro.core.scope;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.diff.Change;
import gyro.core.diff.Delete;
import gyro.core.diff.Replace;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
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
import gyro.util.Bug;

public class State {

    private final RootScope root;
    private final boolean test;
    private final Map<String, FileScope> states = new HashMap<>();
    private final Set<String> diffFiles;
    private final Map<String, String> newNames = new HashMap<>();
    private final Map<String, String> newKeys = new HashMap<>();

    public State(RootScope current, RootScope pending, boolean test, Set<String> diffFiles) {
        this.root = new RootScope(current.getFile(), current.getBackend(), null, current.getLoadFiles());

        root.evaluate();

        this.test = test;
        this.diffFiles = diffFiles != null ? ImmutableSet.copyOf(diffFiles) : null;

        for (FileScope state : root.getFileScopes()) {
            states.put(state.getFile(), state);
        }

        for (FileScope state : pending.getFileScopes()) {
            String stateFile = state.getFile();

            if (!states.containsKey(stateFile)) {
                states.put(stateFile, new FileScope(root, stateFile));
            }
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
                for (FileScope state : states.values()) {
                    String key = resource.primaryKey();
                    state.remove(newKeys.getOrDefault(key, key));
                }

            } else {
                states.values()
                    .stream()
                    .flatMap(s -> s.values().stream())
                    .filter(Resource.class::isInstance)
                    .map(Resource.class::cast)
                    .filter(r -> r.equals(resource.parentResource()))
                    .forEach(r -> updateSubresource(r, resource, true));
            }

        } else {
            FileScope state = states.get(DiffableInternals.getScope(resource).getFileScope().getFile());

            if (typeRoot) {
                String key = resource.primaryKey();
                String newKey = newKeys.getOrDefault(key, key);

                state.put(newKey, resource);

                Resource oldResource = state.getRootScope().findResource(newKey);

                if (oldResource != null) {
                    FileScope oldState = states.get(DiffableInternals.getScope(oldResource).getFileScope().getFile());

                    if (state != oldState) {
                        oldState.remove(newKey);
                    }
                }

            } else {
                String key = resource.parentResource().primaryKey();
                updateSubresource((Resource) state.get(newKeys.getOrDefault(key, key)), resource, false);
            }
        }
    }

    private void updateSubresource(Resource parent, Resource subresource, boolean delete) {
        DiffableField field = DiffableType.getInstance(parent.getClass()).getField(DiffableInternals.getName(subresource));
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

    public void save() {
        NodePrinter printer = new NodePrinter();

        for (FileScope state : states.values()) {
            String file = state.getFile();

            List<Resource> resources = state.values()
                .stream()
                .filter(Resource.class::isInstance)
                .map(Resource.class::cast)
                .collect(Collectors.toList());

            if (!resources.isEmpty()) {
                try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(
                        root.openOutput(file),
                        StandardCharsets.UTF_8))) {

                    PrinterContext context = new PrinterContext(out, 0);

                    for (Resource resource : resources) {
                        printer.visit(
                            new ResourceNode(
                                DiffableType.getInstance(resource.getClass()).getName(),
                                new ValueNode(newNames.getOrDefault(resource.primaryKey(), DiffableInternals.getName(resource))),
                                toBodyNodes(resource)),
                            context);
                    }

                } catch (IOException error) {
                    throw new Bug(error);
                }

            } else {
                root.delete(file);
            }
        }
    }

    private List<Node> toBodyNodes(Diffable diffable) {
        List<Node> body = new ArrayList<>();
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);

        if (!configuredFields.isEmpty()) {
            body.add(toPairNode("_configured-fields", configuredFields));
        }

        body.addAll(DiffableInternals.getScope(diffable).getStateNodes());

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

            } else if (value instanceof Enum<?>) {
                body.add(toPairNode(key, ((Enum) value).name()));

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
                throw new GyroException(String.format(
                    "Can't convert @|bold %s|@, an instance of @|bold %s|@, into a node!",
                    value,
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

            if (DiffableInternals.isExternal(resource)) {
                return new ValueNode(type.getIdField().getValue(resource));

            } else {
                return new ReferenceNode(
                    Arrays.asList(
                        new ValueNode(type.getName()),
                        new ValueNode(newNames.getOrDefault(resource.primaryKey(), DiffableInternals.getName(resource)))),
                    Collections.emptyList());
            }

        } else {
            throw new GyroException(String.format(
                "Can't convert @|bold %s|@, an instance of @|bold %s|@, into a node!",
                value,
                value.getClass().getName()));
        }
    }

    public void replace(Resource resource, Resource with) {
        String resourceType = DiffableType.getInstance(resource.getClass()).getName();
        String withType = DiffableType.getInstance(with.getClass()).getName();

        if (!Objects.equals(resourceType, withType)) {
            throw new GyroException(String.format(
                "Can't replace a [%s] resource with a [%s] resource!",
                resourceType,
                withType));
        }

        String resourceKey = resource.primaryKey();
        String withKey = with.primaryKey();

        states.values().forEach(s -> s.remove(resourceKey));
        newNames.put(withKey, DiffableInternals.getName(resource));
        newKeys.put(withKey, resourceKey);
        save();
    }

}
