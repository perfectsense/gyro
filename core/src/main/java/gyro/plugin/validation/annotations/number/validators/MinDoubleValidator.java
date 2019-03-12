package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.MinDouble;

public class MinDoubleValidator implements AnnotationProcessor<MinDouble, Double> {
    private MinDouble annotation;

    @Override
    public void initialize(MinDouble annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.minMaxValidator(value, annotation.value(), Double.class, false);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
