package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;

public interface RepeatableValidator<A extends Annotation> {
    List<String> getValidations(A annotation, Object value);
}
