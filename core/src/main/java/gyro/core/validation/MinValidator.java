package gyro.core.validation;

public class MinValidator extends AbstractValidator<Min> {

    @Override
    protected boolean validate(Min annotation, Object value) {
        return value instanceof Number && annotation.value() <= ((Number) value).doubleValue();
    }

    @Override
    public String getMessage(Min annotation) {
        return String.format("Must be greater than or equal to @|bold %s|@", annotation.value());
    }

}
