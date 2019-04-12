package gyro.core.diff;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.ResourceId;
import gyro.core.resource.ResourceName;

public class DiffableType<R extends Diffable> {

    private static final LoadingCache<Class<? extends Diffable>, DiffableType<? extends Diffable>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Diffable>, DiffableType<? extends Diffable>>() {

                @Override
                public DiffableType<? extends Diffable> load(Class<? extends Diffable> diffableClass) throws IntrospectionException {
                    return new DiffableType<>(diffableClass);
                }
            });

    private final boolean subresource;
    private final DiffableField idField;
    private final List<DiffableField> fields;
    private final Map<String, DiffableField> fieldByJavaName;
    private final Map<String, DiffableField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <R extends Diffable> DiffableType<R> getInstance(Class<R> diffableClass) {
        try {
            return (DiffableType<R>) INSTANCES.get(diffableClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private DiffableType(Class<R> diffableClass) throws IntrospectionException {
        this.subresource = Optional.ofNullable(diffableClass.getAnnotation(ResourceName.class))
            .map(ResourceName::parent)
            .filter(n -> !ObjectUtils.isBlank(n))
            .isPresent();

        DiffableField idField = null;
        ImmutableList.Builder<DiffableField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, DiffableField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, DiffableField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(diffableClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    DiffableField field = new DiffableField(prop.getName(), getter, setter, getterType);

                    if (getter.isAnnotationPresent(ResourceId.class)) {
                        idField = field;
                    }

                    fields.add(field);
                    fieldByJavaName.put(field.getJavaName(), field);
                    fieldByGyroName.put(field.getGyroName(), field);
                }
            }
        }

        this.idField = idField;
        this.fields = fields.build();
        this.fieldByJavaName = fieldByJavaName.build();
        this.fieldByGyroName = fieldByGyroName.build();
    }

    public boolean isSubresource() {
        return subresource;
    }

    public DiffableField getIdField() {
        return idField;
    }

    public List<DiffableField> getFields() {
        return fields;
    }

    public DiffableField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public DiffableField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

}
