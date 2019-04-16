package gyro.core.validation;

public class RequiredValidator extends AbstractValidator<Required> {
    @Override
    public boolean validate(Required annotation, Object value) {
        return ValidationUtils.isNotNullOrEmpty(value);
    }

    @Override
    public String getMessage(Required annotation) {
        return "Required field.";
    }
}
