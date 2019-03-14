package gyro.core.validations;

import java.util.Arrays;

public class AllowedStringsValidator extends AnnotationBaseProcessor<AllowedStrings> {
    private static AllowedStringsValidator constructor = new AllowedStringsValidator();

    private AllowedStringsValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        String valueCheck = (String) value;

        return Arrays.asList(annotation.value()).contains(valueCheck);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
