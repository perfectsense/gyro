package gyro.core.resource;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.validation.ValidationError;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class DiffableType<D extends Diffable> {

    private static final LoadingCache<Class<? extends Diffable>, DiffableType<? extends Diffable>> INSTANCES = CacheBuilder
        .newBuilder()
        .build(new CacheLoader<Class<? extends Diffable>, DiffableType<? extends Diffable>>() {

            @Override
            public DiffableType<? extends Diffable> load(Class<? extends Diffable> diffableClass) {
                return new DiffableType<>(diffableClass);
            }
        });

    private final Class<D> diffableClass;
    private final boolean root;
    private final String name;
    private final Node description;
    private final DiffableField idField;
    private final List<DiffableField> fields;
    private final Map<String, DiffableField> fieldByName;
    private final List<ModificationField> modificationFields;
    private final Map<String, ModificationField> modificationFieldByName;

    @SuppressWarnings("unchecked")
    public static <T extends Diffable> DiffableType<T> getInstance(Class<T> diffableClass) {
        return (DiffableType<T>) INSTANCES.getUnchecked(diffableClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Diffable> DiffableType<T> getInstance(T diffable) {
        return (DiffableType<T>) INSTANCES.getUnchecked(diffable.getClass());
    }

    private DiffableType(Class<D> diffableClass) {
        this.diffableClass = diffableClass;

        Optional<String> type = Reflections.getTypeOptional(diffableClass);

        if (type.isPresent()) {
            this.root = true;
            this.name = Reflections.getNamespace(diffableClass) + "::" + type.get();

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
        this.modificationFields = new ArrayList<>();
        this.modificationFieldByName = new HashMap<>();

        if (isModifier()) {
            modify();
        }
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
        return Stream.concat(fields.stream(), modificationFields.stream())
            .collect(Collectors.toList());
    }

    public DiffableField getField(String name) {
        return Stream.of(fieldByName, modificationFieldByName)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        .get(name);
    }

    public D newInstance(DiffableScope scope) {
        D diffable = Reflections.newInstance(diffableClass);
        diffable.scope = scope;
        return diffable;
    }

    public String getDescription(D diffable) {
        Map<String, Object> values = new HashMap<>();

        for (DiffableField field : getFields()) {
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

    @SuppressWarnings("unchecked")
    public void setValues(D diffable, Map<String, Object> values) {
        if (diffable.configuredFields == null) {
            diffable.configuredFields = new LinkedHashSet<>(
                Optional.ofNullable((Collection<String>) values.get("_configured-fields"))
                    .orElseGet(values::keySet));
        }

        Set<String> invalidFieldNames = values.keySet()
            .stream()
            .filter(n -> !n.startsWith("_"))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (DiffableField field : getFields()) {
            String fieldName = field.getName();

            if (values.containsKey(fieldName)) {
                field.setValue(diffable, values.get(fieldName));
                invalidFieldNames.remove(fieldName);
            }
        }

        if (!invalidFieldNames.isEmpty()) {
            throw new GyroException(
                values instanceof Scope
                    ? ((Scope) values).getKeyNodes().get(invalidFieldNames.iterator().next())
                    : null,
                String.format(
                    "Following fields aren't valid in @|bold %s|@ type! @|bold %s|@",
                    name,
                    String.join(", ", invalidFieldNames)));
        }

        DiffableInternals.update(diffable, false);
    }

    public List<ValidationError> validate(D diffable) {
        List<ValidationError> errors = new ArrayList<>();
        validateValue(errors, diffable, null, diffable);
        return errors;
    }

    private void validateValue(List<ValidationError> errors, Diffable parent, String name, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Collection) {
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

            Optional.ofNullable(diffable.validate()).ifPresent(errors::addAll);

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

    private boolean isModifier() {
        return Optional.ofNullable(diffableClass.getAnnotation(Modify.class)).isPresent();
    }

    private void modify() {
        Class<? extends Diffable> modifiedClass = Optional.ofNullable(diffableClass.getAnnotation(Modify.class))
            .map(Modify::value)
            .orElse(null);

        if (modifiedClass != null) {
            DiffableType<? extends Diffable> modifiedType = getInstance(modifiedClass);
            for (DiffableField field : getFields()) {
                ModificationField modificationField = new ModificationField(field, this);
                modifiedType.modificationFields.add(modificationField);
                modifiedType.modificationFieldByName.put(modificationField.getName(), modificationField);
            }
        }
    }

}
