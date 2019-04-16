package gyro.core.validation;

public class MinValidator extends AbstractValidator<Min> {
    @Override
    protected boolean validate(Min annotation, Object value) {
        if (value instanceof Number) {
            double valueCheck = ((Number) value).doubleValue();

            return valueCheck >= annotation.value();
        }

        return true;
    }

    @Override
    public String getMessage(Min annotation) {
        return String.format("Minimum allowed number is %s.", annotation.value());
    }
}
