package gyro.core.finder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.Converter;

public class FinderField {

    private static final Converter CONVERTER;

    static {
        CONVERTER = new Converter();
        CONVERTER.setThrowError(true);
        CONVERTER.putAllStandardFunctions();
    }

    private final String javaName;
    private final String gyroName;
    private final Method getter;
    private final Method setter;
    private final String filterName;
    private final Class<?> itemClass;

    protected FinderField(String javaName, Method getter, Method setter, Type type) {
        this.javaName = javaName;
        this.gyroName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;

        Filter filterable = getter.getAnnotation(Filter.class);
        if (filterable != null) {
            this.filterName = filterable.value();
        } else {
            this.filterName = getGyroName();
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

    public String getGyroName() {
        return gyroName;
    }

    public Class<?> getItemClass() {
        return itemClass;
    }

    public String getFilterName() {
        return filterName;
    }

    public Object getValue(Finder query) {
        try {
            return getter.invoke(query);

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    public void setValue(Finder query, Object value) {
        try {
            if (value instanceof Collection
                    && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])) {

                value = ((Collection<?>) value).stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            }

            setter.invoke(query, CONVERTER.convert(setter.getGenericParameterTypes()[0], value));

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
