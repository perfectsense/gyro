package beam.lang;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ResourceType<R extends Resource> {

    private static final LoadingCache<Class<? extends Resource>, ResourceType<? extends Resource>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Resource>, ResourceType<? extends Resource>>() {

                @Override
                public ResourceType<? extends Resource> load(Class<? extends Resource> resourceClass) throws IntrospectionException {
                    return new ResourceType<>(resourceClass);
                }
            });

    private final Class<R> resourceClass;
    private final List<ResourceField> fields;
    private final Map<String, ResourceField> fieldByJavaName;
    private final Map<String, ResourceField> fieldByBeamName;

    @SuppressWarnings("unchecked")
    public static <R extends Resource> ResourceType<R> getInstance(Class<R> resourceClass) {
        try {
            return (ResourceType<R>) INSTANCES.get(resourceClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private ResourceType(Class<R> resourceClass) throws IntrospectionException {
        this.resourceClass = resourceClass;

        ImmutableList.Builder<ResourceField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, ResourceField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, ResourceField> fieldByBeamName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(resourceClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    ResourceField field = new ResourceField(prop.getName(), getter, setter, getterType);

                    fields.add(field);
                    fieldByJavaName.put(field.getJavaName(), field);
                    fieldByBeamName.put(field.getBeamName(), field);
                }
            }
        }

        this.fields = fields.build();
        this.fieldByJavaName = fieldByJavaName.build();
        this.fieldByBeamName = fieldByBeamName.build();
    }

    public List<ResourceField> getFields() {
        return fields;
    }

    public ResourceField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public ResourceField getFieldByBeamName(String beamName) {
        return fieldByBeamName.get(beamName);
    }

}
