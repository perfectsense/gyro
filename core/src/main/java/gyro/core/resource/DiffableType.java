package gyro.core.resource;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.NamespaceUtils;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.scope.DiffableScope;

public class DiffableType<R extends Diffable> {

    private static final LoadingCache<Class<? extends Diffable>, DiffableType<? extends Diffable>> INSTANCES = CacheBuilder
        .newBuilder()
        .build(new CacheLoader<Class<? extends Diffable>, DiffableType<? extends Diffable>>() {

            @Override
            public DiffableType<? extends Diffable> load(Class<? extends Diffable> diffableClass) {
                return new DiffableType<>(diffableClass);
            }
        });

    private final Class<R> diffableClass;
    private final boolean root;
    private final String name;
    private final DiffableField idField;
    private final List<DiffableField> fields;
    private final Map<String, DiffableField> fieldByName;

    @SuppressWarnings("unchecked")
    public static <R extends Diffable> DiffableType<R> getInstance(Class<R> diffableClass) {
        return (DiffableType<R>) INSTANCES.getUnchecked(diffableClass);
    }

    private DiffableType(Class<R> diffableClass) {
        this.diffableClass = diffableClass;

        Type typeAnnotation = diffableClass.getAnnotation(Type.class);

        if (typeAnnotation != null) {
            this.root = true;
            this.name = NamespaceUtils.getNamespacePrefix(diffableClass)+ typeAnnotation.value();

        } else {
            this.root = false;
            this.name = null;
        }

        DiffableField idField = null;
        ImmutableList.Builder<DiffableField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, DiffableField> fieldByName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Reflections.getBeanInfo(diffableClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                java.lang.reflect.Type getterType = getter.getGenericReturnType();
                java.lang.reflect.Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    DiffableField field = new DiffableField(prop.getName(), getter, setter, getterType);

                    if (getter.isAnnotationPresent(Id.class)) {
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

    public R newDiffable(Diffable parent, String name, DiffableScope scope) {
        R diffable = Reflections.newInstance(diffableClass);
        diffable.parent = parent;
        diffable.name = name;
        diffable.scope = scope;

        return diffable;
    }

}
