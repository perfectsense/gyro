package gyro.core.resource;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.NamespaceUtils;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.validation.ValidationError;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

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
    private final Node description;
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

        this.description = Optional.ofNullable(diffableClass.getAnnotation(Description.class))
            .map(Description::value)
            .map(v -> Node.parse('"' + v + '"', GyroParser::string))
            .orElse(null);

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

    public String getDescription(Diffable diffable) {
        Map<String, Object> values = new HashMap<>();

        for (DiffableField field : fields) {
            values.put(field.getName(), field.getValue(diffable));
        }

        DiffableScope scope = diffable.scope;
        RootScope root = scope.getRootScope();

        Node node = scope.getSettings(DescriptionSettings.class).getDescription();

        if (node == null) {
            node = root.getSettings(DescriptionSettings.class).getTypeDescriptions().get(name);

            if (node == null) {
                node = description;
            }
        }

        return description != null
            ? (String) root.getEvaluator().visit(node, new Scope(root, values))
            : null;
    }

    public R newDiffable(Diffable parent, String name, DiffableScope scope) {
        R diffable = Reflections.newInstance(diffableClass);
        diffable.parent = parent;
        diffable.name = name;
        diffable.scope = scope;

        return diffable;
    }

    public List<ValidationError> validate(Diffable diffable) {
        List<ValidationError> errors = new ArrayList<>();
        validateValue(errors, diffable, null, diffable);
        return errors;
    }

    private void validateValue(List<ValidationError> errors, Diffable parent, String name, Object value) {
        if (value == null) {
            errors.add(new ValidationError(
                parent,
                name,
                "Can't validate a null!"));

        } else if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                validateValue(errors, parent, name, item);
            }

        } else if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;

            for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
                errors.addAll(field.validate(diffable));

                if (field.shouldBeDiffed()) {
                    validateValue(errors, diffable, field.getName(), field.getValue(diffable));
                }
            }

            Optional.ofNullable(diffable.validations()).ifPresent(errors::addAll);

        } else {
            errors.add(new ValidationError(
                parent,
                name,
                String.format(
                    "Can't validate @|bold %s|@, an instance of @|bold %s|@!",
                    value,
                    value.getClass().getName())));
        }
    }

}
