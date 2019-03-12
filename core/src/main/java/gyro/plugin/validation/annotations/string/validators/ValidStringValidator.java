package gyro.plugin.validation.annotations.string.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.string.ValidString;

public class ValidStringValidator implements AnnotationProcessor<ValidString, String> {
    private ValidString annotation;

    @Override
    public void initialize(ValidString annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return false;
        }

        String valueCheck = (String) value;

        String validStringValue = annotation.value();

        return validStringValue.equals(valueCheck);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
