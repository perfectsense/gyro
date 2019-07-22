package gyro.core.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.base.CaseFormat;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.util.ConversionException;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.Scope;
import gyro.core.validation.ValidatorClass;
import gyro.core.validation.Validator;

public class DiffableField {

    private final String name;
    private final Method getter;
    private final Method setter;
    private final boolean updatable;
    private final String testValue;
    private final boolean testValueRandomSuffix;
    private final boolean collection;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;
        this.updatable = getter.isAnnotationPresent(Updatable.class);

        Output output = getter.getAnnotation(Output.class);

        if (output != null) {
            this.testValue = !ObjectUtils.isBlank(output.value()) ? output.value() : name;
            this.testValueRandomSuffix = output.randomSuffix();

        } else {
            this.testValue = null;
            this.testValueRandomSuffix = false;
        }

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

    public String getName() {
        return name;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public String getTestValue() {
        return testValue;
    }

    public boolean isTestValueRandomSuffix() {
        return testValueRandomSuffix;
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

            Reflections.invoke(setter, diffable, scope.getRootScope().convertValue(type, value));

        } catch (ConversionException error) {
            throw new GyroException(
                scope.getKeyNodes().get(name),
                String.format("Can't set @|bold %s|@ to @|bold %s|@ because it can't be converted to an instance of @|bold %s|@!",
                    name,
                    value,
                    type.getTypeName()));
        }
    }

    public List<String> validate(Diffable diffable) {
        List<String> validationMessages = new ArrayList<>();

        Object object = this.getValue(diffable);

        // New Way
        /*try {
            Stream.of(getter.getAnnotations())
                .map(Annotation::annotationType)
                .map(a -> a.getAnnotation(AnnotationProcessorClass.class))
                .filter(Objects::nonNull)
                .map(AnnotationProcessorClass::value)
                .map(this::getValidator)
                .filter(Objects::nonNull)
                .forEach(
                    v -> {
                        if (!v.isValid(o -> o, object)) {

                        }
                    }
                );
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }*/

        // Old way
        for (Annotation annotation : getter.getAnnotations()) {
            ValidatorClass validatorClass = annotation.annotationType().getAnnotation(ValidatorClass.class);
            if (validatorClass != null) {
                try {
                    Validator validator = (Validator) SINGLETONS.get(validatorClass.value());
                    if (!validator.isValid(annotation, object)) {
                        validationMessages.add(validator.getMessage(annotation));
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return validationMessages;
    }


    // Helper new way
    private Validator getValidator(Class itemClass) {
        try {
            return (Validator) SINGLETONS.get(itemClass);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static final LoadingCache<Class<?>, Object> SINGLETONS = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new CacheLoader<Class<?>, Object>() {
            public Object load(Class<?> c) throws IllegalAccessException, InstantiationException {
                return c.newInstance();
            }
        });

}
