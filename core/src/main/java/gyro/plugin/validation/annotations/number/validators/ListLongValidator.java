package gyro.plugin.validation.annotations.number.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.number.ListLong;

import java.util.Arrays;

public class ListLongValidator implements AnnotationProcessor<ListLong, Long> {
    private ListLong annotation;

    @Override
    public void initialize(ListLong annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        return NumberValidatorHelper.listValidator(value, annotation.value(), Long.class);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
