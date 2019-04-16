package gyro.core.validation;

public class MinValidator extends AbstractValidator<Min> {
    @Override
    boolean validate(Object value) {
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);

            return valueCheck >= annotation.value();
        }

        return true;
    }

    @Override
    public String getMessage() {
        return String.format("Minimum allowed number is %s.", annotation.value());
    }
}
