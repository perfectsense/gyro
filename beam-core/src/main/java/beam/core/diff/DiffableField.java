package beam.core.diff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.Converter;

public class DiffableField {

    private static final Converter CONVERTER;

    static {
        CONVERTER = new Converter();
        CONVERTER.setThrowError(true);
        CONVERTER.putAllStandardFunctions();
    }

    private final String javaName;
    private final String beamName;
    private final Method getter;
    private final Method setter;
    private final boolean nullable;
    private final boolean updatable;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.javaName = javaName;
        this.beamName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;

        ResourceDiffProperty annotation = getter.getAnnotation(ResourceDiffProperty.class);

        if (annotation != null) {
            this.nullable = annotation.nullable();
            this.updatable = annotation.updatable();

        } else {
            this.nullable = false;
            this.updatable = false;
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

    public String getJavaName() {
        return javaName;
    }

    public String getBeamName() {
        return beamName;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public Class<?> getItemClass() {
        return itemClass;
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

    public void setValue(Diffable diffable, Object value) {
        try {
            if (value instanceof List
                    && !List.class.isAssignableFrom(setter.getParameterTypes()[0])) {

                value = ((List<?>) value).stream()
                        .findFirst()
                        .orElse(null);
            }

            setter.invoke(diffable, CONVERTER.convert(setter.getGenericParameterTypes()[0], value));

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

}