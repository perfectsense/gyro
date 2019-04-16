package gyro.core.validation;

public class MaxValidator extends AbstractValidator<Max> {
    @Override
    boolean validate(Object value) {
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);

            return valueCheck <= annotation.value();
        }

        return true;
    }

    @Override
    public String getMessage() {
        return String.format("Maximum allowed number is %s.", annotation.value());
    }
}
