package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;

public class AllowedStringsValidator extends AbstractValidator<AllowedStrings> {
    @Override
    boolean validate(Object value) {
        HashSet<String> validValues = new HashSet(Arrays.asList(annotation.value()));

        if (value instanceof String) {
            return validValues.contains(value);
        }

        return true;
    }

    @Override
    public String getMessage() {
        return String.format("Valid value should be on of %s.",Arrays.toString(annotation.value()));
    }
}
