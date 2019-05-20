package gyro.core.finder;

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

public class FinderType<R extends Finder> {

    private static final LoadingCache<Class<? extends Finder>, FinderType<? extends Finder>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Finder>, FinderType<? extends Finder>>() {

                @Override
                public FinderType<? extends Finder> load(Class<? extends Finder> queryClass) throws IntrospectionException {
                    return new FinderType<>(queryClass);
                }
            });

    private final List<FinderField> fields;
    private final Map<String, FinderField> fieldByJavaName;
    private final Map<String, FinderField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <R extends Finder> FinderType<R> getInstance(Class<R> queryClass) {
        try {
            return (FinderType<R>) INSTANCES.get(queryClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private FinderType(Class<R> queryClass) throws IntrospectionException {
        ImmutableList.Builder<FinderField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, FinderField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, FinderField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(queryClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    FinderField field = new FinderField(prop.getName(), getter, setter, getterType);

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

    public List<FinderField> getFields() {
        return fields;
    }

    public FinderField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public FinderField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

}
