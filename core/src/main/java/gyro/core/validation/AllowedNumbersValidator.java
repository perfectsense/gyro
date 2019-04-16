package gyro.core.validation;

import com.google.common.primitives.Doubles;

import java.util.Arrays;
import java.util.HashSet;

public class AllowedNumbersValidator extends AbstractValidator<AllowedNumbers> {
    @Override
    boolean validate(AllowedNumbers annotation, Object value) {
        HashSet<Double> validValues = new HashSet(Doubles.asList(annotation.value()));

        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);
            return validValues.contains(valueCheck);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedNumbers annotation) {
        return String.format("Valid number should be on of %s.", Arrays.toString(annotation.value()));
    }
}
