package gyro.core.validation;

import java.util.Arrays;

public class AllowedRegexValidator extends AbstractValidator<AllowedRegex> {
    @Override
    protected boolean validate(AllowedRegex annotation, Object value) {
        if (value instanceof String) {
            return Arrays.stream(annotation.value()).anyMatch(((String) value)::matches);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedRegex annotation) {
        return String.format("Valid value should be one of these formats %s.",
            annotation.display().length == 0 ? Arrays.toString(annotation.value()) : Arrays.toString(annotation.display()));
    }
}
