package beam.parser;

import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.ConfigKey;
import beam.core.diff.ResourceDiffProperty;
import com.psddev.dari.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeamConfigGenerator implements ConfigGenerator {

    @Override
    public String generate(Object resource) {
        if (resource instanceof BeamResource) {
            Class<?> resourceClass = resource.getClass();

            try {
                Map propertyMap = new HashMap();
                String config = "";
                for (PropertyDescriptor p : Introspector.getBeanInfo(resourceClass).getPropertyDescriptors()) {
                    Method reader = p.getReadMethod();
                    Method writer = p.getWriteMethod();
                    if (reader != null && writer != null) {
                        ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
                        if (propertyAnnotation != null) {
                            if (!ObjectUtils.isBlank(reader.invoke(resource))) {
                                propertyMap.put(p.getName(), reader.invoke(resource));
                            }
                        }
                    }
                }

                String propertyJson = ObjectUtils.toJson(propertyMap);
                Object map = ObjectUtils.fromJson(propertyJson);
                String packageName = resourceClass.getName().split("\\.")[1];
                ConfigKey configKey = resourceClass.getAnnotation(ConfigKey.class);

                if (configKey != null) {
                    String provider = String.format("%s::%s", packageName, configKey.value());
                    config = provider + " " + fromMap((Map) map);
                }

                return config;

            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException error) {
                throw new BeamException(String.format("Unable to generate configs for %s", resourceClass.getName()), error);
            }
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported resource type: %s", resource.getClass()));
        }
    }

    private Object fromMap(Map map) {
        return fromMap(map, 0);
    }

    private String indentation(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++){
            sb.append(" ");
        }

        return sb.toString();
    }

    private String fromMap(Map map, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Object key : map.keySet()) {
            sb.append("\n");
            sb.append(indentation(indent + 4));
            sb.append(key.toString());
            sb.append(": ");
            Object value = map.get(key);
            if (value instanceof Map) {
                sb.append(fromMap((Map) value, indent + 4));
            } else if (value instanceof List) {
                sb.append(fromList((List) value, indent + 4));
            } else {
                if (value != null) {
                    String convertString = value.toString();
                    if (value instanceof String) {
                        convertString = "\"" + convertString + "\"";
                    }

                    sb.append(convertString);
                }
            }
        }

        sb.append("\n");
        sb.append(indentation(indent));
        sb.append("}");
        return sb.toString();
    }

    private String fromList(List list, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            sb.append("\n");
            sb.append(indentation(indent + 4));
            if (value instanceof Map) {
                sb.append(fromMap((Map) value, indent + 4));
            } else if (value instanceof List) {
                sb.append(fromList((List) value, indent + 4));
            } else {
                if (value != null) {
                    String convertString = value.toString();
                    if (value instanceof String) {
                        convertString = "\"" + convertString + "\"";
                    }

                    sb.append(convertString);
                } else {
                    sb.append("null");
                }
            }

            if (i < list.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("\n");
        sb.append(indentation(indent));
        sb.append("]");
        return sb.toString();
    }
}
