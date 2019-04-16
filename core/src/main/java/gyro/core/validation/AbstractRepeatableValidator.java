package gyro.core.validation;

import java.lang.annotation.Annotation;

public abstract class AbstractRepeatableValidator<A extends Annotation> implements RepeatableValidator<A> {
    public A annotation;
}
