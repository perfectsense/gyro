package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidator<A extends Annotation> implements Validator<A> {
    A annotation;

    abstract boolean validate(A annotation, Object value);

    @Override
    public boolean isValid(A annotation, Object value) {
        this.annotation = annotation;

        if (value == null) {
            return true;
        } else if (value instanceof List && ((List) value).size() > 0) {
            for (Object object : (List) value) {
                if (!isValid(annotation, object)) {
                    return false;
                }
            }
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0) {
            for (Object object : ((Map) value).keySet()) {
                if (!isValid(annotation, object)) {
                    return false;
                }
            }
        } else {
            return validate(annotation, value);
        }

        return true;
    }
}
