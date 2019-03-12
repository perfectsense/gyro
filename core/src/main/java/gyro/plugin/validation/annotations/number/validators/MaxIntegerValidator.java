package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.MaxInteger;

public class MaxIntegerValidator implements AnnotationProcessor<MaxInteger, Integer> {
    private MaxInteger annotation;

    @Override
    public void initialize(MaxInteger annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.minMaxValidator(value, annotation.value(), Integer.class, true);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
