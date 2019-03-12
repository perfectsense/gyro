package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.RangeLong;

public class RangeLongValidator implements AnnotationProcessor<RangeLong, Long> {
    private RangeLong annotation;

    @Override
    public void initialize(RangeLong annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.rangeValidator(value, annotation.low(), annotation.high(), Long.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.low(), annotation.high());
    }
}
