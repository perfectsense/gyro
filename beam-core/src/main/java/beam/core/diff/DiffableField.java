package beam.core.diff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import beam.lang.Resource;
import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;

public class DiffableField {

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

    public Object getValue(Resource resource) {
        try {
            return getter.invoke(resource);

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    public void setValue(Resource resource, Object value) {
        try {
            setter.invoke(resource, ObjectUtils.to(setter.getGenericParameterTypes()[0], value));

        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    public boolean isSubresource() {
        return Resource.class.isAssignableFrom(itemClass);
    }

}
