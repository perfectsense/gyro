package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AnnotationNumberBaseProcessor<A extends Annotation> implements AnnotationProcessor<A> {
    A annotation;

    boolean isDouble = false;

    abstract boolean doValidation(Object value);

    @Override
    public void initialize(A annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return true;
        }

        isDouble = isDouble(value);

        return doValidation(value);
    }

    private boolean isDouble(Object value) {
        return (value instanceof Double)
            || ((value instanceof List) && (((List) value).size() > 0) && (((List) value).get(0) instanceof Double))
            || ((value instanceof Map) && ((((Map) value).keySet().size() > 0)) && (((Map) value).keySet().toArray()[0] instanceof Double));
    }
}
