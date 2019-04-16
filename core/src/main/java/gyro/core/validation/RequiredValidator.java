package gyro.core.validation;

public class RequiredValidator implements Validator<Required> {
    private Required annotation;
    private static RequiredValidator constructor = null;

    private RequiredValidator() {
    }

    public static Validator getAnnotationProcessor() {
        if (constructor == null) {
            constructor = new RequiredValidator();
        }

        return constructor;
    }

    @Override
    public boolean isValid(Required annotation, Object value) {
        this.annotation = annotation;

        return ValidationUtils.isNotNullOrEmpty(value);
    }

    @Override
    public String getMessage() {
        return annotation.message();
    }
}
