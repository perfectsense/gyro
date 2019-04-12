package gyro.core.query;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.lang.ResourceFinder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class QueryType<R extends ResourceFinder> {

    private static final LoadingCache<Class<? extends ResourceFinder>, QueryType<? extends ResourceFinder>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends ResourceFinder>, QueryType<? extends ResourceFinder>>() {

                @Override
                public QueryType<? extends ResourceFinder> load(Class<? extends ResourceFinder> queryClass) throws IntrospectionException {
                    return new QueryType<>(queryClass);
                }
            });

    private final List<QueryField> fields;
    private final Map<String, QueryField> fieldByJavaName;
    private final Map<String, QueryField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <R extends ResourceFinder> QueryType<R> getInstance(Class<R> queryClass) {
        try {
            return (QueryType<R>) INSTANCES.get(queryClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private QueryType(Class<R> queryClass) throws IntrospectionException {
        ImmutableList.Builder<QueryField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, QueryField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, QueryField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(queryClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    QueryField field = new QueryField(prop.getName(), getter, setter, getterType);

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

    public List<QueryField> getFields() {
        return fields;
    }

    public QueryField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public QueryField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

}
