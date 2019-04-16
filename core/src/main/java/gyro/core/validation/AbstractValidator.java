package gyro.core.validation;

import java.lang.annotation.Annotation;

public abstract class AbstractValidator<A extends Annotation> implements Validator<A> {
    A annotation;

    abstract boolean validate(Object value);

    @Override
    public boolean isValid(A annotation, Object value) {
        this.annotation = annotation;

        if (value == null) {
            return true;
        }

        return validate(value);
    }
}
