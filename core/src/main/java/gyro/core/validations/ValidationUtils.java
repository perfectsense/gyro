package gyro.core.validations;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class ValidationUtils {

    public enum DependencyType { ALLOWED, REQUIRED }

    static String getFieldName(String fieldName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, fieldName).replace("get-", "");
    }

    static boolean isNullOrEmpty(Object value) {
        if (value != null) {
            if (value instanceof List) {
                return !((List) value).isEmpty();
            } else if (value instanceof Map) {
                return !((Map) value).isEmpty();
            } else if (value instanceof String) {
                return !ObjectUtils.isBlank(value);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    static Object getValueFromField(String fieldName, Diffable diffable) throws InvocationTargetException,
        IllegalArgumentException, NoSuchMethodException, IllegalAccessException {
        String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return getValueFromMethod(methodName, diffable);
    }

    static Object getValueFromMethod(String methodName, Diffable diffable) throws InvocationTargetException,
        IllegalArgumentException, NoSuchMethodException, IllegalAccessException {
        Method method = diffable.getClass().getMethod(methodName);
        return method.invoke(diffable);
    }
}