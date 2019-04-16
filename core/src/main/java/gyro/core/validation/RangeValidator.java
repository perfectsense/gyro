package gyro.core.validation;

public class RangeValidator extends AbstractValidator<Range> {
    @Override
    boolean validate(Range annotation, Object value) {
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);

            return valueCheck >= annotation.low() && valueCheck <= annotation.high();
        }

        return true;
    }

    @Override
    public String getMessage(Range annotation) {
        return String.format("Valid number should be in the range of [ %s - %s ].",annotation.low(), annotation.high());
    }
}
