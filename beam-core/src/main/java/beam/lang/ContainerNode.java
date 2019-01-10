package beam.lang;

import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import org.apache.commons.beanutils.BeanUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerNode extends Node {

    transient Map<String, ValueNode> keyValues = new HashMap<>();
    transient List<ControlNode> controlNodes = new ArrayList<>();

    private static final Pattern NEWLINES = Pattern.compile("([\r\n]+)");

    /**
     * Returns a map of key/value pairs for this block.
     *
     * This map is the internal state with the properties (from subclasses)
     * overlaid.
     *
     * The values are after resolution.
     */
    public Map<String, Object> resolvedKeyValues() {
        Map<String, Object> values = new HashMap<>();

        for (String key : keys()) {
            values.put(key, get(key).getValue());
        }

        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass(), ContainerNode.class).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                if (reader != null) {
                    String key = keyFromFieldName(p.getDisplayName());
                    Object value = reader.invoke(this);

                    values.put(key, value);
                }
            }
        } catch (IllegalAccessException | IntrospectionException error) {
            throw new IllegalStateException(error);
        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        return values;
    }

    public Set<String> keys() {
        return keyValues.keySet();
    }

    public ValueNode get(String key) {
        return keyValues.get(key);
    }

    public void put(String key, ValueNode valueNode) {
        valueNode.setParentNode(this);

        keyValues.put(key, valueNode);
    }

    public void putControlNode(ControlNode node) {
        controlNodes.add(node);
    }

    public List<ControlNode> controlNodes() {
        return controlNodes;
    }

    public void copyNonResourceState(ContainerNode source) {
        keyValues.putAll(source.keyValues);
    }

    public void evaluateControlNodes() {
        for (ControlNode controlNode : controlNodes()) {
            controlNode.evaluate();
        }
    }

    public ContainerNode copy() {
        try {
            ContainerNode node = getClass().newInstance();

            for (String key : keys()) {
                ValueNode valueNode = get(key).copy();
                valueNode.setParentNode(node);

                node.put(key, valueNode);
            }

            return node;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeamException("");
        }
    }

    protected void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    ValueNode valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.getLine());

                    throw new BeamException(message);
                }

                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }
    }


    @Override
    public boolean resolve() {
        for (ValueNode value : keyValues.values()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        for (ControlNode controlNode : controlNodes()) {
            boolean resolved = controlNode.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", controlNode);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> entry : resolvedKeyValues().entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            // If there is no getter for this method then skip this field since there can
            // be no ResourceDiffProperty annotation.
            Method reader = readerMethodForKey(entry.getKey());

            // If no ResourceDiffProperty annotation or if this field is not subresources then skip this field.
            ResourceDiffProperty propertyAnnotation = reader != null ? reader.getAnnotation(ResourceDiffProperty.class) : null;
            if (propertyAnnotation != null && propertyAnnotation.subresource()) {
                if (value instanceof List) {
                    for (Object resource : (List) value) {
                        sb.append(subresourceToString((BeamResource) resource));
                    }
                } else if (value instanceof BeamResource) {
                    sb.append(subresourceToString((BeamResource) value));
                }
            } else {
                sb.append("    ").append(entry.getKey()).append(": ");

                if (value instanceof String) {
                    sb.append("'" + entry.getValue() + "'");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(entry.getValue());
                } else if (value instanceof Map) {
                    sb.append(mapToString((Map) value));
                } else if (value instanceof List) {
                    sb.append(listToString((List) value));
                } else if (value instanceof ResourceNode) {
                    sb.append(((ResourceNode) value).resourceKey());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    protected String subresourceToString(BeamResource resource) {
        StringBuilder sb = new StringBuilder();
        int offset = 0;

        String output = resource.toString();
        for (Matcher m = NEWLINES.matcher(output); m.find();) {
            sb.append("    ");
            sb.append(output.substring(offset, m.start()));
            sb.append(m.group(1));

            offset = m.end();
        }

        return sb.toString();
    }

    protected String valueToString(Object value) {
        StringBuilder sb = new StringBuilder();

        if (value instanceof String) {
            sb.append("'" + value + "'");
        } else if (value instanceof Map) {
            sb.append(mapToString((Map) value));
        } else if (value instanceof List) {
            sb.append(listToString((List) value));
        }

        return sb.toString();
    }

    protected String mapToString(Map map) {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append("\n");

        if (!map.isEmpty()) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);

                sb.append("        ");
                sb.append(key).append(": ");
                sb.append(valueToString(value));
                sb.append(",\n");
            }
            sb.setLength(sb.length() - 2);
        }

        sb.append("\n    }");

        return sb.toString();
    }

    protected String listToString(List list) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append("\n");

        if (!list.isEmpty()) {
            for (Object value : list) {
                sb.append("        ");
                sb.append(valueToString(value));
                sb.append(",\n");
            }
            sb.setLength(sb.length() - 2);
        }

        sb.append("\n    ]");

        return sb.toString();
    }

    protected String fieldNameFromKey(String key) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
    }

    protected String keyFromFieldName(String field) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field).replaceFirst("get-", "");
    }

    protected Method readerMethodForKey(String key) {
        String convertedKey = fieldNameFromKey(key);
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                if (p.getDisplayName().equals(convertedKey)) {
                    return p.getReadMethod();
                }
            }
        } catch (IntrospectionException ex) {
            // Ignoring introspection exceptions
        }

        return null;
    }

}
