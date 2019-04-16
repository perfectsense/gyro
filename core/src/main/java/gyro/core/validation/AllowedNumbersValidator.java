package gyro.core.validation;

import java.util.Arrays;

public class AllowedNumbersValidator extends AbstractValidator<AllowedNumbers> {
    @Override
    boolean validate(AllowedNumbers annotation, Object value) {
        if (value instanceof Number) {
            double valueCheck = ((Number) value).doubleValue();
            return Arrays.stream(annotation.value()).anyMatch(o -> o == valueCheck);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedNumbers annotation) {
        return String.format("Valid number should be one of %s.", Arrays.toString(annotation.value()));
    }
}
