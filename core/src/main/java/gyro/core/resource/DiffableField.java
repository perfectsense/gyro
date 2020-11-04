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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.util.ConversionException;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.Scope;
import gyro.core.validation.Required;
import gyro.core.validation.ValidationError;
import gyro.core.validation.Validator;
import gyro.core.validation.ValidatorClass;

public class DiffableField {

    private static final LoadingCache<Class<? extends Validator<? extends Annotation>>, Validator<Annotation>> VALIDATORS = CacheBuilder
        .newBuilder()
        .weakKeys()
        .build(new CacheLoader<Class<? extends Validator<? extends Annotation>>, Validator<Annotation>>() {

            @Override
            @SuppressWarnings("unchecked")
            public Validator<Annotation> load(Class<? extends Validator<? extends Annotation>> c) {
                return (Validator) Reflections.newInstance(c);
            }
        });

    private final String name;
    private final Method getter;
    private final Method setter;
    private final boolean updatable;
    private final boolean calculated;
    private final boolean immutable;
    private final boolean output;
    private final boolean required;
    private final boolean collection;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;
        this.updatable = isAnnotationPresent(getter, Updatable.class);
        this.calculated = isAnnotationPresent(getter, Calculated.class);
        this.immutable = isAnnotationPresent(getter, Immutable.class);
        this.output = isAnnotationPresent(getter, Output.class);
        this.required = isAnnotationPresent(getter, Required.class);

        if (type instanceof Class) {
            this.collection = false;
            this.itemClass = (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            this.collection = Collection.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
            this.itemClass = Optional.of(parameterizedType)
                .map(ParameterizedType::getActualTypeArguments)
                .filter(args -> args.length > 0)
                .map(args -> args[0])
                .filter(a0 -> a0 instanceof Class)
                .map(Class.class::cast)
                .orElseThrow(UnsupportedOperationException::new);

        } else {
            throw new GyroException(String.format(
                "@|bold %s|@ isn't supported as a field type!",
                type.getTypeName()));
        }
    }

    protected DiffableField(DiffableField field) {
        name = field.name;
        getter = field.getter;
        setter = field.setter;
        updatable = field.updatable;
        calculated = field.calculated;
        immutable = field.immutable;
        output = field.output;
        required = field.required;
        collection = field.collection;
        itemClass = field.itemClass;
    }

    public String getName() {
        return name;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public boolean isOutput() {
        return output;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCollection() {
        return collection;
    }

    public Class<?> getItemClass() {
        return itemClass;
    }

    @SuppressWarnings("unchecked")
    public boolean shouldBeDiffed() {
        return Diffable.class.isAssignableFrom(itemClass)
            && !DiffableType.getInstance((Class<? extends Diffable>) itemClass).isRoot();
    }

    public Object getValue(Diffable diffable) {
        return Reflections.invoke(getter, diffable);
    }

    public void setValue(Diffable diffable, Object value) {
        Scope scope = diffable.scope;
        Type type = setter.getGenericParameterTypes()[0];

        try {
            if (value instanceof Collection
                && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])) {

                value = ((Collection<?>) value).stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            }

            Object convertedValue = scope.getRootScope().convertValue(type, value);

            if (value != null && convertedValue == null && (type instanceof Class && ((Class<?>) type).isEnum())) {
                Object[] enumConstants = ((Class) type).getEnumConstants();

                throw new GyroException(
                    scope.getLocation(name),
                    "Must be one of " + Stream.of(enumConstants)
                        .map(v -> String.format("@|bold %s|@", v))
                        .collect(Collectors.joining(", "))
                );
            }

            Reflections.invoke(setter, diffable, convertedValue);

        } catch (ConversionException error) {
            throw new GyroException(
                scope.getLocation(name),
                String.format(
                    "Can't set @|bold %s|@ to @|bold %s|@ because it can't be converted to an instance of @|bold %s|@!",
                    name,
                    value,
                    type.getTypeName()));
        }
    }

    public void testUpdate(Diffable diffable) {
        if (!isAnnotationPresent(getter, Output.class)) {
            return;
        }

        Optional<Object> testValue = Optional.ofNullable(getAnnotation(getter, TestValue.class)).map(TestValue::value);

        if (Date.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(Date::new));

        } else if (Number.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(() -> Math.random() * Integer.MAX_VALUE));

        } else if (String.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(() -> String.join(
                "-",
                "test",
                name,
                UUID.randomUUID().toString().replace("-", "").substring(16))));
        }
    }

    public List<ValidationError> validate(Diffable diffable) {
        Object value = getValue(diffable);
        List<ValidationError> errors = new ArrayList<>();

        for (Annotation annotation : getAnnotations(getter)) {
            ValidatorClass validatorClass = annotation.annotationType().getAnnotation(ValidatorClass.class);

            if (validatorClass != null) {
                Validator<Annotation> validator = VALIDATORS.getUnchecked(validatorClass.value());

                if (!validator.isValid(diffable, annotation, value)) {
                    errors.add(new ValidationError(diffable, name, validator.getMessage(annotation)));
                }
            }
        }

        return errors;
    }

    protected static boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass) {
        if (!method.isAnnotationPresent(annotationClass)) {
            Method superMethod = getSuperMethod(method);

            if (superMethod != null) {
                return isAnnotationPresent(superMethod, annotationClass);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    protected static List<Annotation> getAnnotations(Method method) {
        List<Annotation> annotations = new ArrayList<>(Arrays.asList(method.getAnnotations()));
        Method superMethod = getSuperMethod(method);

        if (superMethod != null) {
            annotations.addAll(getAnnotations(superMethod));
        }

        return annotations;
    }

    protected static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        if (method.isAnnotationPresent(annotationClass)) {
            return method.getAnnotation(annotationClass);
        } else {
            Method superMethod = getSuperMethod(method);

            if (superMethod != null) {
                return getAnnotation(superMethod, annotationClass);
            } else {
                return null;
            }
        }
    }

    private static Method getSuperMethod(Method method) {
        Class<?> superclass = method.getDeclaringClass().getSuperclass();
        if (superclass != null) {
            try {
                return superclass.getMethod(method.getName());
            } catch (NoSuchMethodException ignore) {
                return null;
            }
        } else {
            return null;
        }
    }
}
