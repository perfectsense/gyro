package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;

public class AllowedStringsValidator extends AbstractValidator<AllowedStrings> {
    @Override
    boolean validate(AllowedStrings annotation, Object value) {
        HashSet<String> validValues = new HashSet(Arrays.asList(annotation.value()));

        if (value instanceof String) {
            return validValues.contains(value);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedStrings annotation) {
        return String.format("Valid value should be on of %s.",Arrays.toString(annotation.value()));
    }
}
