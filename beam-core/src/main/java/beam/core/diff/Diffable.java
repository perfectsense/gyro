package beam.core.diff;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import beam.lang.Resource;
import beam.lang.ast.KeyValueNode;
import beam.lang.ast.Node;
import beam.lang.ast.block.KeyBlockNode;
import beam.lang.ast.scope.DiffableScope;
import beam.lang.ast.value.BooleanNode;
import beam.lang.ast.value.ListNode;
import beam.lang.ast.value.MapNode;
import beam.lang.ast.value.NumberNode;
import beam.lang.ast.value.StringNode;

public abstract class Diffable {

    private Diffable parent;
    private Change change;

    public Diffable parent() {
        return parent;
    }

    public Resource parentResource() {
        for (Diffable d = parent(); d != null; d = d.parent()) {
            if (d instanceof Resource) {
                return (Resource) d;
            }
        }

        return null;
    }

    public Diffable parent(Diffable parent) {
        this.parent = parent;
        return this;
    }

    public Change change() {
        return change;
    }

    public void change(Change change) {
        this.change = change;
    }

    public void initialize(Map<String, Object> values) {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();

            if (!values.containsKey(key)) {
                continue;
            }

            Class<?> itemClass = field.getItemClass();
            Object value = values.get(key);

            if (Diffable.class.isAssignableFrom(itemClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Diffable> diffableClass = (Class<? extends Diffable>) itemClass;

                if (value instanceof List) {
                    value = ((List<?>) value).stream()
                            .map(v -> toDiffable(key, diffableClass, v))
                            .collect(Collectors.toList());

                } else if (value instanceof DiffableScope) {
                    value = toDiffable(key, diffableClass, value);
                }
            }

            field.setValue(this, value);
        }
    }

    private Object toDiffable(String key, Class<? extends Diffable> diffableClass, Object object) {
        if (!(object instanceof DiffableScope)) {
            return object;
        }

        DiffableScope scope = (DiffableScope) object;
        Diffable diffable;

        try {
            diffable = diffableClass.getConstructor().newInstance();

        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }

        if (diffable instanceof Resource) {
            Resource valueResource = (Resource) diffable;

            valueResource.parent((Resource) this);
            valueResource.resourceType(key);
            valueResource.scope(scope);
            valueResource.initialize(scope);
        }

        diffable.initialize(scope);

        return diffable;
    }

    public abstract String primaryKey();

    public abstract String toDisplayString();

    public List<Node> toBodyNodes() {
        List<Node> body = new ArrayList<>();

        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            Object value = field.getValue(this);

            if (value == null) {
                continue;
            }

            String key = field.getBeamName();

            if (value instanceof Boolean) {
                body.add(new KeyValueNode(key, new BooleanNode(Boolean.TRUE.equals(value))));

            } else if (value instanceof Date) {
                body.add(new KeyValueNode(key, new StringNode(value.toString())));

            } else if (value instanceof Diffable) {
                body.add(new KeyBlockNode(key, ((Diffable) value).toBodyNodes()));

            } else if (value instanceof List) {
                if (Diffable.class.isAssignableFrom(field.getItemClass())) {
                    for (Object item : (List<?>) value) {
                        body.add(new KeyBlockNode(key, ((Diffable) item).toBodyNodes()));
                    }

                } else {
                    body.add(new KeyValueNode(key, toNode(value)));
                }

            } else if (value instanceof Map) {
                body.add(new KeyValueNode(key, toNode(value)));

            } else if (value instanceof Number) {
                body.add(new KeyValueNode(key, new NumberNode((Number) value)));

            } else if (value instanceof String) {
                body.add(new KeyValueNode(key, new StringNode((String) value)));

            } else {
                throw new UnsupportedOperationException(String.format(
                        "Can't convert an instance of [%s] into a node!",
                        value.getClass().getName()));
            }
        }

        return body;
    }

    private Node toNode(Object value) {
        if (value instanceof Boolean) {
            return new BooleanNode(Boolean.TRUE.equals(value));

        } else if (value instanceof List) {
            List<Node> items = new ArrayList<>();

            for (Object item : (List<?>) value) {
                items.add(toNode(item));
            }

            return new ListNode(items);

        } else if (value instanceof Map) {
            List<KeyValueNode> entries = new ArrayList<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                entries.add(new KeyValueNode(
                        (String) entry.getKey(),
                        toNode(entry.getValue())));
            }

            return new MapNode(entries);

        } else if (value instanceof Number) {
            return new NumberNode((Number) value);

        } else if (value instanceof String) {
            return new StringNode((String) value);

        } else {
            throw new UnsupportedOperationException(String.format(
                    "Can't convert an instance of [%s] into a node!",
                    value.getClass().getName()));
        }
    }

}
