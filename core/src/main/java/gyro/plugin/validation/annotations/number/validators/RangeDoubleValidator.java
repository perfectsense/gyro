package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.RangeDouble;

public class RangeDoubleValidator implements AnnotationProcessor<RangeDouble, Double> {
    private RangeDouble annotation;

    @Override
    public void initialize(RangeDouble annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.rangeValidator(value, annotation.low(), annotation.high(), Double.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.low(), annotation.high());
    }
}
