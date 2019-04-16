package gyro.core.validation;

import java.util.Arrays;
import java.util.List;

public class AllowedRegexValidator extends AbstractValidator<AllowedRegex> {
    @Override
    boolean validate(AllowedRegex annotation, Object value) {
        List<String> validValues = Arrays.asList(annotation.value());

        if (value instanceof String) {
            return validValues.stream().anyMatch(((String) value)::matches);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedRegex annotation) {
        return String.format("Valid value should be one of these formats %s.",
            annotation.display().length == 0 ? Arrays.toString(annotation.value()) : Arrays.toString(annotation.display()));
    }
}
