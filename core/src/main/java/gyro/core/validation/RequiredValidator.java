package gyro.core.validation;

public class RequiredValidator implements AnnotationProcessor<Required> {
    private Required annotation;
    private static RequiredValidator constructor = null;

    private RequiredValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        if (constructor == null) {
            constructor = new RequiredValidator();
        }

        return constructor;
    }

    @Override
    public boolean isValid(Object value) {
        return ValidationUtils.isNotNullOrEmpty(value);
    }

    @Override
    public void initialize(Required annotation) {
        this.annotation = annotation;
    }

    @Override
    public String getMessage() {
        return annotation.message();
    }
}
