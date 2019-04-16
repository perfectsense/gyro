package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;

public interface RepeatableValidator<A extends Annotation> {
    void initialize(A annotation);

    List<String> getValidations(Object value);
}
