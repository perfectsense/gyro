package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.MinLong;

public class MinLongValidator implements AnnotationProcessor<MinLong, Long> {
    private MinLong annotation;

    @Override
    public void initialize(MinLong annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.minMaxValidator(value, annotation.value(), Long.class, false);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.value());
    }
}
