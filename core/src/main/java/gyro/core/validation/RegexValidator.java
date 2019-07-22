package gyro.core.validation;

import java.util.Arrays;

public class RegexValidator extends AbstractValidator<Regex> {

    @Override
    protected boolean validate(Regex annotation, Object value) {
        if (value instanceof String) {
            return Arrays.stream(annotation.value()).anyMatch(((String) value)::matches);
        }

        return true;
    }

    @Override
    public String getMessage(Regex annotation) {
        return String.format(
            "Valid value should be one of these formats %s.",
            annotation.display().length == 0
                ? Arrays.toString(annotation.value())
                : Arrays.toString(annotation.display()));
    }

}
