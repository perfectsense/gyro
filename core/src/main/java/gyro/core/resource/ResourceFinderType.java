package gyro.core.resource;

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

public class ResourceFinderType<R extends ResourceFinder> {

    private static final LoadingCache<Class<? extends ResourceFinder>, ResourceFinderType<? extends ResourceFinder>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends ResourceFinder>, ResourceFinderType<? extends ResourceFinder>>() {

                @Override
                public ResourceFinderType<? extends ResourceFinder> load(Class<? extends ResourceFinder> queryClass) throws IntrospectionException {
                    return new ResourceFinderType<>(queryClass);
                }
            });

    private final List<ResourceFinderField> fields;
    private final Map<String, ResourceFinderField> fieldByJavaName;
    private final Map<String, ResourceFinderField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <R extends ResourceFinder> ResourceFinderType<R> getInstance(Class<R> queryClass) {
        try {
            return (ResourceFinderType<R>) INSTANCES.get(queryClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private ResourceFinderType(Class<R> queryClass) throws IntrospectionException {
        ImmutableList.Builder<ResourceFinderField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, ResourceFinderField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, ResourceFinderField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(queryClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    ResourceFinderField field = new ResourceFinderField(prop.getName(), getter, setter, getterType);

                    fields.add(field);
                    fieldByJavaName.put(field.getJavaName(), field);
                    fieldByGyroName.put(field.getGyroName(), field);
                }
            }
        }

        this.fields = fields.build();
        this.fieldByJavaName = fieldByJavaName.build();
        this.fieldByGyroName = fieldByGyroName.build();
    }

    public List<ResourceFinderField> getFields() {
        return fields;
    }

    public ResourceFinderField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public ResourceFinderField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

}
