package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;

public class RequiredValidator extends AbstractValidator<Required> {
    @Override
    public boolean validate(Required annotation, Object value) {
        return ObjectUtils.isBlank(value);
    }

    @Override
    public String getMessage(Required annotation) {
        return "Required field.";
    }
}
