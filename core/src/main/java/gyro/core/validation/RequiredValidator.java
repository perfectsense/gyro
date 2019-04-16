package gyro.core.validation;

public class RequiredValidator implements Validator<Required> {
    @Override
    public boolean isValid(Required annotation, Object value) {
        return ValidationUtils.isNotNullOrEmpty(value);
    }

    @Override
    public String getMessage(Required annotation) {
        return "Required field.";
    }
}
