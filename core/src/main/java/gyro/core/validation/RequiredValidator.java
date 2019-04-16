package gyro.core.validation;

public class RequiredValidator implements Validator<Required> {
    private Required annotation;

    @Override
    public boolean isValid(Required annotation, Object value) {
        this.annotation = annotation;

        return ValidationUtils.isNotNullOrEmpty(value);
    }

    @Override
    public String getMessage() {
        return "Required field.";
    }
}
