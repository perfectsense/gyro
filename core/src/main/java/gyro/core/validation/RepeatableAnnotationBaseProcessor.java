package gyro.core.validation;

import java.lang.annotation.Annotation;

public abstract class RepeatableAnnotationBaseProcessor<A extends Annotation> implements RepeatableAnnotationProcessor<A> {
    public A annotation;

    @Override
    public void initialize(A annotation) {
        this.annotation = annotation;
    }
}