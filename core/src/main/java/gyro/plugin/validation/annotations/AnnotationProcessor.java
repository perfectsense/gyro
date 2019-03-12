package gyro.plugin.validation.annotations;

import java.lang.annotation.Annotation;

public interface AnnotationProcessor<A extends Annotation, T> {
    void initialize(A annotation);

    boolean isValid(Object value);

    String getMessage();
}
