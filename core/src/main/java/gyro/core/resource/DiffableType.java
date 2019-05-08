package gyro.core.resource;

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

public class DiffableType<R extends Diffable> {

    private static final LoadingCache<Class<? extends Diffable>, DiffableType<? extends Diffable>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Diffable>, DiffableType<? extends Diffable>>() {

                @Override
                public DiffableType<? extends Diffable> load(Class<? extends Diffable> diffableClass) throws IntrospectionException {
                    return new DiffableType<>(diffableClass);
                }
            });

    private static final LoadingCache<ClassLoader, LoadingCache<String, String>> NAMESPACES_BY_LOADER = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new CacheLoader<ClassLoader, LoadingCache<String, String>>() {

            @Override
            public LoadingCache<String, String> load(ClassLoader loader) {
                return CacheBuilder.newBuilder()
                    .build(new CacheLoader<String, String>() {

                        @Override
                        public String load(String name) {
                            Package pkg;

                            try {
                                pkg = Class.forName(name + ".package-info", true, loader).getPackage();

                            } catch (ClassNotFoundException error) {
                                pkg = null;
                            }

                            return Optional.ofNullable(pkg)
                                .map(p -> p.getAnnotation(ResourceNamespace.class))
                                .map(ResourceNamespace::value)
                                .orElseGet(() -> {
                                    int lastDotAt = name.lastIndexOf('.');

                                    return lastDotAt > -1
                                        ? NAMESPACES_BY_LOADER.getUnchecked(loader).getUnchecked(name.substring(0, lastDotAt))
                                        : "";
                                });
                        }
                    });
            }
        });

    private final boolean root;
    private final String name;
    private final DiffableField idField;
    private final List<DiffableField> fields;
    private final Map<String, DiffableField> fieldByName;

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
        ResourceType typeAnnotation = diffableClass.getAnnotation(ResourceType.class);

        if (typeAnnotation != null) {
            String namespace = Optional.ofNullable(diffableClass.getAnnotation(ResourceNamespace.class))
                .map(ResourceNamespace::value)
                .orElseGet(() -> {
                    Package pkg = diffableClass.getPackage();

                    return pkg != null
                        ? NAMESPACES_BY_LOADER.getUnchecked(diffableClass.getClassLoader()).getUnchecked(pkg.getName())
                        : "";
                });

            if (!namespace.isEmpty()) {
                namespace += "::";
            }

            this.root = true;
            this.name = namespace + typeAnnotation.value();

        } else {
            this.root = false;
            this.name = null;
        }

        DiffableField idField = null;
        ImmutableList.Builder<DiffableField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, DiffableField> fieldByName = ImmutableMap.builder();

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
                    fieldByName.put(field.getName(), field);
                }
            }
        }

        this.idField = idField;
        this.fields = fields.build();
        this.fieldByName = fieldByName.build();
    }

    public boolean isRoot() {
        return root;
    }

    public String getName() {
        return name;
    }

    public DiffableField getIdField() {
        return idField;
    }

    public List<DiffableField> getFields() {
        return fields;
    }

    public DiffableField getField(String name) {
        return fieldByName.get(name);
    }

}
