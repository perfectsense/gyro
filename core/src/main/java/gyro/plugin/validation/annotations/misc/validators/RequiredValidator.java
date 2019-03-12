package gyro.plugin.validation.annotations.misc.validators;

import com.psddev.dari.util.ObjectUtils;
import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.misc.Required;

import java.util.List;
import java.util.Map;

public class RequiredValidator implements AnnotationProcessor<Required, Object> {
    private Required annotation;

    @Override
    public void initialize(Required annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
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

    @Override
    public String getMessage() {
        return annotation.message();
    }
}
