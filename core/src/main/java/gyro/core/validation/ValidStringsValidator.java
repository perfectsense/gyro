package gyro.core.validation;

import java.util.Arrays;

public class ValidStringsValidator extends AbstractValidator<ValidStrings> {

    @Override
    protected boolean validate(ValidStrings annotation, Object value) {
        if (value instanceof String) {
            return Arrays.asList(annotation.value()).contains(value);
        }

        return true;
    }

    @Override
    public String getMessage(ValidStrings annotation) {
        return String.format("Valid value should be one of %s.", Arrays.toString(annotation.value()));
    }

}
