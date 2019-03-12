package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.RangeInteger;

public class RangeIntegerValidator implements AnnotationProcessor<RangeInteger, Integer> {
    private RangeInteger annotation;

    @Override
    public void initialize(RangeInteger annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.rangeValidator(value, annotation.low(), annotation.high(), Integer.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.low(), annotation.high());
    }
}
