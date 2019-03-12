package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.ListDouble;

import java.util.Arrays;

public class ListDoubleValidator implements AnnotationProcessor<ListDouble, Double> {
    private ListDouble annotation;

    @Override
    public void initialize(ListDouble annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.listValidator(value, annotation.value(), Double.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
