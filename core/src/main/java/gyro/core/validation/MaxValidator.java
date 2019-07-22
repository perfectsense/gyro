package gyro.core.validation;

public class MaxValidator extends AbstractValidator<Max> {

    @Override
    protected boolean validate(Max annotation, Object value) {
        return value instanceof Number && ((Number) value).doubleValue() <= annotation.value();
    }

    @Override
    public String getMessage(Max annotation) {
        return String.format("Must be less than or equal to @|bold %s|@", annotation.value());
    }

}
