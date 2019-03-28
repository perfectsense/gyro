package gyro.core.validation;

import java.lang.annotation.Annotation;

public abstract class AnnotationBaseProcessor<A extends Annotation> implements AnnotationProcessor<A> {
    A annotation;

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

        return doValidation(value);
    }
}
