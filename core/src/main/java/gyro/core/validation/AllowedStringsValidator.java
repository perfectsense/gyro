package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AllowedStringsValidator extends AnnotationBaseProcessor<AllowedStrings> {
    private static AllowedStringsValidator constructor = new AllowedStringsValidator();

    private AllowedStringsValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        HashSet<String> validValues = new HashSet(Arrays.asList(annotation.value()));
        if (value instanceof String) {
            String valueCheck = (String) value;

            return Arrays.asList(annotation.value()).contains(valueCheck);
        } else if (value instanceof List && ((List) value).size() > 0 && ((List) value).get(0) instanceof String) {
            return ((List) value).stream().allMatch(validValues::contains);
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0 && ((Map) value).keySet().toArray()[0] instanceof String) {
            return ((Map) value).keySet().stream().allMatch(validValues::contains);
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
