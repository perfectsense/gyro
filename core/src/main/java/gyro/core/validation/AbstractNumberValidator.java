package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AbstractNumberValidator<A extends Annotation> extends AbstractValidator<A> {
    boolean isDouble = false;

    @Override
    public boolean isValid(A annotation, Object value) {
        this.annotation = annotation;

        if (value == null) {
            return true;
        }

        setIsDouble(value);

        return validate(value);
    }

    private void setIsDouble(Object value) {
        isDouble = (value instanceof Double)
            || ((value instanceof List) && (((List) value).size() > 0) && (((List) value).get(0) instanceof Double))
            || ((value instanceof Map) && ((((Map) value).keySet().size() > 0)) && (((Map) value).keySet().toArray()[0] instanceof Double));
    }
}
