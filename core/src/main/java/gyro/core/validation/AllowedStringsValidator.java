package gyro.core.validation;

import java.util.Arrays;

public class AllowedStringsValidator extends AbstractValidator<AllowedStrings> {
    @Override
    protected boolean validate(AllowedStrings annotation, Object value) {
        if (value instanceof String) {
            return Arrays.asList(annotation.value()).contains(value);
        }

        return true;
    }

    @Override
    public String getMessage(AllowedStrings annotation) {
        return String.format("Valid value should be one of %s.",Arrays.toString(annotation.value()));
    }
}
