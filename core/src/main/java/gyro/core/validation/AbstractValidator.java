package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidator<A extends Annotation> implements Validator<A> {

    protected abstract boolean validate(A annotation, Object value);

    @Override
    public final boolean isValid(A annotation, Object value) {
        if (value instanceof List) {
            return ((List<?>) value).stream().allMatch(o -> isValid(annotation, o));

        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).keySet().stream().allMatch(o -> isValid(annotation, o));

        } else {
            return validate(annotation, value);
        }
    }

}
