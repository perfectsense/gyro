package gyro.core.validation;

public class MaxValidator extends AbstractValidator<Max> {

    @Override
    protected boolean validate(Max annotation, Object value) {
        if (value instanceof Number) {
            double valueCheck = ((Number) value).doubleValue();
            return valueCheck <= annotation.value();
        }

        return true;
    }

    @Override
    public String getMessage(Max annotation) {
        return String.format("Maximum allowed number is %s.", annotation.value());
    }

}
