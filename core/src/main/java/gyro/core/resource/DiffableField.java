package gyro.core.resource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.util.ConversionException;
import com.psddev.dari.util.Converter;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.validation.AnnotationProcessorClass;
import gyro.core.validation.Validator;
import gyro.lang.ast.Node;

import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class DiffableField {

    private static final Converter CONVERTER;

    static {
        CONVERTER = new Converter();
        CONVERTER.setThrowError(true);
        CONVERTER.putAllStandardFunctions();

        CONVERTER.putDirectFunction(
            Resource.class,
            String.class,
            (converter, returnType, resource) -> ObjectUtils.to(
                String.class,
                DiffableType.getInstance(resource.getClass())
                    .getIdField()
                    .getValue(resource)));
    }

    private final String name;
    private final Method getter;
    private final Method setter;
    private final boolean updatable;
    private final String testValue;
    private final boolean testValueRandomSuffix;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;
        this.updatable = getter.isAnnotationPresent(ResourceUpdatable.class);

        ResourceOutput output = getter.getAnnotation(ResourceOutput.class);

        if (output != null) {
            this.testValue = !ObjectUtils.isBlank(output.value()) ? output.value() : name;
            this.testValueRandomSuffix = output.randomSuffix();
        } else {
            this.testValue = null;
            this.testValueRandomSuffix = false;
        }

        if (type instanceof Class) {
            this.itemClass = (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            this.itemClass = Optional.of((ParameterizedType) type)
                    .map(ParameterizedType::getActualTypeArguments)
                    .filter(args -> args.length > 0)
                    .map(args -> args[0])
                    .filter(a0 -> a0 instanceof Class)
                    .map(Class.class::cast)
                    .orElseThrow(UnsupportedOperationException::new);

        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getName() {
        return name;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public Class<?> getItemClass() {
        return itemClass;
    }

    public String getTestValue() {
        return testValue;
    }

    public boolean isTestValueRandomSuffix() {
        return testValueRandomSuffix;
    }

    @SuppressWarnings("unchecked")
    public boolean shouldBeDiffed() {
        return Diffable.class.isAssignableFrom(itemClass)
            && !DiffableType.getInstance((Class<? extends Diffable>) itemClass).isRoot();
    }

    public Object getValue(Diffable diffable) {
        try {
            return getter.invoke(diffable);

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    @SuppressWarnings("unchecked")
    public void setValue(Diffable diffable, Object value) {
        Scope scope = diffable.scope;
        Node node = scope != null ? scope.getKeyNodes().get(getName()) : null;

        try {
            if (value instanceof Collection
                    && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])) {

                value = ((Collection<?>) value).stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            }

            if (value instanceof String && Resource.class.isAssignableFrom(itemClass)) {
                value = diffable.findById((Class<? extends Resource>) itemClass, (String) value);
            }

            setter.invoke(diffable, CONVERTER.convert(setter.getGenericParameterTypes()[0], value));

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        } catch (ConversionException e) {
            if (node != null) {
                throw new GyroException(String.format("Type mismatch when setting field '%s' %s%n%s.%n",
                    getName(), node.getLocation(), node));
            }

            throw new GyroException(String.format("Type mismatch when setting field '%s' with '%s'.",
                getName(), value));
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
            AnnotationProcessorClass annotationProcessorClass = annotation.annotationType().getAnnotation(AnnotationProcessorClass.class);
            if (annotationProcessorClass != null) {
                try {
                    Validator validator = (Validator) SINGLETONS.get(annotationProcessorClass.value());
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
