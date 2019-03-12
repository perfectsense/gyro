package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.MinInteger;

public class MinIntegerValidator implements AnnotationProcessor<MinInteger, Integer> {
    private MinInteger annotation;

    @Override
    public void initialize(MinInteger annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.minMaxValidator(value, annotation.value(), Integer.class, false);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
