package gyro.core.validations;

import com.psddev.dari.util.ObjectUtils;

import java.util.List;
import java.util.Map;

public class RequiredValidator extends AnnotationBaseProcessor<Required> {
    private static RequiredValidator constructor = null;

    private RequiredValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        if (constructor == null) {
            constructor = new RequiredValidator();
        }

        return constructor;
    }

    @Override
    public boolean doValidation(Object value) {
        if (value instanceof List) {
            return !((List) value).isEmpty();
        } else if (value instanceof Map) {
            return !((Map) value).isEmpty();
        } else if (value instanceof String) {
            return !ObjectUtils.isBlank(value);
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return annotation.message();
    }
}
