package gyro.plugin.validation.annotations.string.validators;

import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.string.ListString;

import java.util.Arrays;

public class ListStringValidator implements AnnotationProcessor<ListString, String> {
    private ListString annotation;

    @Override
    public void initialize(ListString annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return false;
        }

        String valueCheck = (String) value;

        String[] validValues = annotation.value();

        return Arrays.asList(validValues).contains(valueCheck);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
