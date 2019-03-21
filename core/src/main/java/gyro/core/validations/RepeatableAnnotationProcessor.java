package gyro.core.validations;

import java.lang.annotation.Annotation;
import java.util.List;

public interface RepeatableAnnotationProcessor<A extends Annotation> {
    void initialize(A annotation);

    List<String> getValidations(Object value);
}
