package gyro.core.validation;

import java.util.Arrays;

public class ValidNumbersValidator extends AbstractValidator<ValidNumbers> {

    @Override
    protected boolean validate(ValidNumbers annotation, Object value) {
        if (value instanceof Number) {
            double valueCheck = ((Number) value).doubleValue();
            return Arrays.stream(annotation.value()).anyMatch(o -> o == valueCheck);
        }

        return true;
    }

    @Override
    public String getMessage(ValidNumbers annotation) {
        return String.format("Valid number should be one of %s.", Arrays.toString(annotation.value()));
    }

}
