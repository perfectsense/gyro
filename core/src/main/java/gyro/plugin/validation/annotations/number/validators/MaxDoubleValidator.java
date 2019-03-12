package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.MaxDouble;

public class MaxDoubleValidator implements AnnotationProcessor<MaxDouble, Double> {
    private MaxDouble annotation;

    @Override
    public void initialize(MaxDouble annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.minMaxValidator(value, annotation.value(), Double.class, true);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
