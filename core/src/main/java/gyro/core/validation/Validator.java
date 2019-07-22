package gyro.core.validation;

import java.lang.annotation.Annotation;

public interface Validator<A extends Annotation> {

    boolean isValid(A annotation, Object value);

    String getMessage(A annotation);

}
