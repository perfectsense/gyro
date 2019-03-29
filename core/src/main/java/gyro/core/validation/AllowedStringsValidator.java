package gyro.core.validation;

import java.util.ArrayList;
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
        List<String> valueChecks = new ArrayList<>();
        if (value instanceof String) {
           valueChecks.add((String) value);
        } else if (value instanceof List && ((List) value).size() > 0 && ((List) value).get(0) instanceof String) {
            valueChecks = (List<String>) value;
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0 && ((Map) value).keySet().toArray()[0] instanceof String) {
            valueChecks = (List<String>) new ArrayList(Arrays.asList(((Map) value).keySet().toArray()));
        }

        if (!valueChecks.isEmpty()) {
            return validValues.containsAll(valueChecks);
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), Arrays.toString(annotation.value()));
    }
}
