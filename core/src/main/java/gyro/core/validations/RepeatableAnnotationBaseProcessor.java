package gyro.core.validations;

import java.lang.annotation.Annotation;

public abstract class RepeatableAnnotationBaseProcessor<A extends Annotation> implements RepeatableAnnotationProcessor<A> {
    A annotation;

    @Override
    public void initialize(A annotation) {
        this.annotation = annotation;
    }
}
