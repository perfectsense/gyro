package gyro.core.validations;

import java.lang.annotation.Annotation;

public interface AnnotationProcessor<A extends Annotation> {
    void initialize(A annotation);

    boolean isValid(Object value);

    String getMessage();
}
