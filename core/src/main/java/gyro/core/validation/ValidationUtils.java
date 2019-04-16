package gyro.core.validation;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class ValidationUtils {
    public static String getFieldName(String fieldName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, fieldName).replace("get-", "");
    }
}
