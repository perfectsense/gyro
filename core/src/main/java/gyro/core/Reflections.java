package gyro.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import gyro.util.Bug;

public class Reflections {

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
