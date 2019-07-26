package gyro.core.validation;

public class RangeValidator extends AbstractValidator<Range> {

    private static final RangesValidator VALIDATOR = new RangesValidator();

    @Override
    protected boolean validate(Range annotation, Object value) {
        if (value instanceof Number) {
            double check = ((Number) value).doubleValue();
            return annotation.min() <= check && check <= annotation.max();
        }

        return false;
    }

    @Override
    public String getMessage(Range annotation) {
        return VALIDATOR.getMessage(annotation);
    }

}