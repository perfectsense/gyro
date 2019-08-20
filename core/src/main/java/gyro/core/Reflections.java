package gyro.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import gyro.util.Bug;
import org.apache.commons.lang3.StringUtils;

public class Reflections {

    public static Optional<String> getTypeOptional(Class<?> aClass) {
        return Optional.ofNullable(aClass.getAnnotation(Type.class))
            .map(Type::value)
            .filter(StringUtils::isNotBlank);
    }

    public static String getType(Class<?> aClass) {
        return getTypeOptional(aClass).orElseThrow(() -> new Bug(String.format(
            "@|bold %s|@ class requires a @Type annotation with a non-blank value!",
            aClass.getName())));
    }

    public static BeanInfo getBeanInfo(Class<?> aClass) {
        try {
            return Introspector.getBeanInfo(aClass);

        } catch (IntrospectionException error) {
            throw new GyroException(
                String.format("Can't introspect @|bold %s|@!", aClass.getName()),
                error);
        }
    }

    public static <T> T newInstance(Class<T> aClass) {
        try {
            return aClass.newInstance();

        } catch (IllegalAccessException | InstantiationException error) {
            throw new Bug(error);
        }
    }

    public static Object invoke(Method method, Object object, Object... arguments) {
        try {
            return method.invoke(object, arguments);

        } catch (IllegalAccessException error) {
            throw new Bug(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new GyroException(cause);
        }
    }

}
