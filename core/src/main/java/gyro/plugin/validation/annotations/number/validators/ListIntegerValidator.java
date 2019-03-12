package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.ListInteger;

import java.util.Arrays;

public class ListIntegerValidator implements AnnotationProcessor<ListInteger, Integer> {
    private ListInteger annotation;

    @Override
    public void initialize(ListInteger annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.listValidator(value, annotation.value(), Integer.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
