package gyro.core.validation;

public class RegexValidator extends AbstractValidator<Regex> {

    private static final RegexesValidator VALIDATOR = new RegexesValidator();

    @Override
    protected boolean validate(Regex annotation, Object value) {
        return value instanceof String && ((String) value).matches(annotation.value());
    }

    @Override
    public String getMessage(Regex annotation) {
        return VALIDATOR.getMessage(annotation);
    }

}
