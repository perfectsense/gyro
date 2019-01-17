package beam.lang;

import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.lang.types.ReferenceValue;
import beam.lang.types.Value;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Container extends Node {

    transient Map<String, Value> keyValues = new HashMap<>();
    transient List<Control> controls = new ArrayList<>();
    transient List<Frame> frames = new ArrayList<>();

    static {
        DateTimeConverter converter = new DateConverter(null);
        ConvertUtils.register(converter, Date.class);
    }

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
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass(), Container.class).getPropertyDescriptors()) {
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

    public Value get(String key) {
        return keyValues.get(key);
    }

    public void put(String key, Value value) {
        value.parent(this);

        keyValues.put(key, value);
    }

    public void putControl(Control node) {
        controls.add(node);
    }

    public List<Control> controls() {
        return controls;
    }

    public List<Frame> frames() {
        return frames;
    }

    public void frames(List<Frame> frames) {
        this.frames = frames;
    }

    public void copyNonResourceState(Container source) {
        keyValues.putAll(source.keyValues);
    }

    protected void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    Value valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.line());

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
        for (Value value : keyValues.values()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        for (Control control : controls()) {
            boolean resolved = control.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", control);
            }
        }

        for (Frame frame : new ArrayList<>(frames())) {
            boolean resolved = frame.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", frame);
            }
        }

        return true;
    }

    @Override
    public String serialize(int indent) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> entry : resolvedKeyValues().entrySet()) {
            Value sourceValue = get(entry.getKey());
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
                        sb.append(((Resource) resource).serialize(indent));
                    }
                } else if (value instanceof Resource) {
                    sb.append(((Resource) value).serialize(indent));
                }
            } else {
                if (value instanceof Map && ((Map) value).isEmpty() || value instanceof List && ((List) value).isEmpty()) {
                    continue;
                }

                sb.append(indent(indent)).append(entry.getKey()).append(": ");

                if (sourceValue instanceof ReferenceValue && ((ReferenceValue) sourceValue).getReferencedContainer() != null) {
                    sb.append(sourceValue.toString());
                } else if (value instanceof String) {
                    sb.append("'").append(entry.getValue()).append("'");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(entry.getValue());
                } else if (value instanceof Map && !((Map) value).isEmpty()) {
                    sb.append(mapToString((Map) value, indent));
                } else if (value instanceof List && !((List) value).isEmpty()) {
                    sb.append(listToString((List) value, indent));
                } else if (value instanceof Resource) {
                    sb.append(((Resource) value).resourceKey());
                } else {
                    sb.append("'").append(value.toString()).append("'");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Container[key/values: %d, controls: %d]", keys().size(), controls().size());
    }

    protected String valueToString(Object value, int indent) {
        StringBuilder sb = new StringBuilder();

        if (value instanceof String) {
            sb.append("'" + value + "'");
        } else if (value instanceof Map) {
            sb.append(mapToString((Map) value, indent));
        } else if (value instanceof List) {
            sb.append(listToString((List) value, indent));
        }

        return sb.toString();
    }

    protected String mapToString(Map map, int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append("\n");

        if (!map.isEmpty()) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);

                sb.append(indent(indent + 4));
                sb.append(key).append(": ");
                sb.append(valueToString(value, indent));
                sb.append(",\n");
            }
            sb.setLength(sb.length() - 2);
        }

        sb.append("\n    }");

        return sb.toString();
    }

    protected String listToString(List list, int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append("\n");

        if (!list.isEmpty()) {
            for (Object value : list) {
                sb.append(indent(indent + 4));
                sb.append(valueToString(value, indent));
                sb.append(",\n");
            }
            sb.setLength(sb.length() - 2);
        }

        sb.append("\n");
        sb.append(indent(indent)).append("]");

        return sb.toString();
    }

    String fieldNameFromKey(String key) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
    }

    String keyFromFieldName(String field) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field).replaceFirst("get-", "");
    }

    Method readerMethodForKey(String key) {
        PropertyDescriptor p = propertyDescriptorForKey(key);
        if (p != null) {
            return p.getReadMethod();
        }

        return null;
    }

    Method writerMethodForKey(String key) {
        PropertyDescriptor p = propertyDescriptorForKey(key);
        if (p != null) {
            return p.getWriteMethod();
        }

        return null;
    }

    PropertyDescriptor propertyDescriptorForKey(String key) {
        String convertedKey = fieldNameFromKey(key);
        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                if (p.getDisplayName().equals(convertedKey)) {
                    return p;
                }
            }
        } catch (IntrospectionException ex) {
            // Ignoring introspection exceptions
        }

        return null;
    }

}
