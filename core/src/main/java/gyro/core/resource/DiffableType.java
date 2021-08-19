/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.resource;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.auth.CredentialsSettings;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.validation.ValidationError;
import gyro.core.workflow.ModifiedIn;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;

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
    private final Set<Class<? extends Modification<D>>> modificationClasses = new HashSet<>();
    private final List<ModificationField> modificationFields = new ArrayList<>();

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

        for (PropertyDescriptor prop : Reflections.getBeanInfo(diffableClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                java.lang.reflect.Type getterType = getter.getGenericReturnType();
                java.lang.reflect.Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    DiffableField field = new DiffableField(prop.getName(), getter, setter, getterType);
                    if (DiffableField.isAnnotationPresent(getter, Id.class)) {
                        idField = field;
                    }

                    fields.add(field);
                }
            }
        }

        this.idField = idField;
        this.fields = fields.build();
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
        ImmutableList.Builder<DiffableField> fields = ImmutableList.builder();

        fields.addAll(this.fields);
        fields.addAll(this.modificationFields);

        return fields.build();
    }

    public DiffableField getField(String name) {
        for (DiffableField field : getFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public D newExternal(RootScope root, Object id) {
        D diffable = Reflections.newInstance(diffableClass);
        diffable.external = true;
        diffable.scope = new DiffableScope(root, null);

        if (idField == null) {
            throw new Bug(String.format("%s is missing @Id annotation.", diffableClass.getName()));
        }

        idField.setValue(diffable, id);

        return diffable;
    }

    public D newExternalWithCredentials(RootScope root, Object id, String credentials) {
        D diffable = newExternal(root, id);
        diffable.scope.getClosest(DiffableScope.class)
            .getSettings(CredentialsSettings.class)
            .setUseCredentials(credentials);

        return diffable;
    }

    public D newInternal(DiffableScope scope, String name) {
        D diffable = Reflections.newInstance(diffableClass);
        diffable.scope = scope;

        for (Class<? extends Modification<D>> modificationClass : modificationClasses) {
            DiffableType<? extends Modification<D>> modificationType = DiffableType.getInstance(modificationClass);

            Modification<D> modification = modificationType.newInternal(
                new DiffableScope(scope, null),
                modificationType.getName() + "::" + name);

            DiffableInternals.getModifications(diffable).add(modification);
        }

        scope.addProcessor(new CalculatedDiffableProcessor());

        diffable.name = name;

        setValues(diffable, scope);
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

        if (diffable.modifiedIn == null) {
            diffable.modifiedIn = Optional.ofNullable(values.get("_modified-in"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(ModifiedIn::fromString)
                .orElse(null);
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
                    ? ((Scope) values).getLocation(invalidFieldNames.iterator().next())
                    : null,
                String.format(
                    "Following fields aren't valid in @|bold %s|@ type! @|bold %s|@",
                    name,
                    String.join(", ", invalidFieldNames)));
        }

        DiffableInternals.update(diffable);
    }

    public List<ValidationError> validate(D diffable) {
        List<ValidationError> errors = new ArrayList<>();
        validateValue(errors, diffable, null, diffable);
        return errors;
    }

    void modify(Class<? extends Modification<D>> modificationClass) {
        if (modificationClasses.add(modificationClass)) {
            DiffableType<? extends Modification<D>> modificationType = DiffableType.getInstance(modificationClass);

            modificationFields.addAll(
                modificationType.getFields()
                    .stream()
                    .map(ModificationField::new)
                    .collect(Collectors.toSet())
            );
        }
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
            Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);

            for (DiffableField field : DiffableType.getInstance(diffable.getClass()).getFields()) {
                if (field.isRequired() || configuredFields.contains(field.getName())) {
                    errors.addAll(field.validate(diffable));

                    if (field.shouldBeDiffed()) {
                        validateValue(errors, diffable, field.getName(), field.getValue(diffable));
                    }
                }
            }

            Optional.ofNullable(diffable.validate(configuredFields)).ifPresent(errors::addAll);

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
