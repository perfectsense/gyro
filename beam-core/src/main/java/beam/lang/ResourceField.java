package beam.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import com.google.common.base.CaseFormat;

public class ResourceField {

    private final String javaName;
    private final String beamName;
    private final Method getter;
    private final Method setter;
    private final boolean subresource;

    protected ResourceField(String javaName, Method getter, Method setter, Type type) {
        this.javaName = javaName;
        this.beamName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;

        if (type instanceof Class) {
            this.subresource = Resource.class.isAssignableFrom((Class<?>) type);

        } else if (type instanceof ParameterizedType) {
            this.subresource = Optional.of((ParameterizedType) type)
                    .map(ParameterizedType::getActualTypeArguments)
                    .filter(args -> args.length > 0)
                    .map(args -> args[0])
                    .filter(a0 -> a0 instanceof Class)
                    .map(Class.class::cast)
                    .filter(Resource.class::isAssignableFrom)
                    .isPresent();

        } else {
            this.subresource = false;
        }
    }

    public String getJavaName() {
        return javaName;
    }

    public String getBeamName() {
        return beamName;
    }

    public boolean isSubresource() {
        return subresource;
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
            setter.invoke(resource, value);

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
