package gyro.core.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AllowedRegexValidator extends AnnotationBaseProcessor<AllowedRegex> {
    private static AllowedRegexValidator constructor = new AllowedRegexValidator();

    private AllowedRegexValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        List<String> validValues = Arrays.asList(annotation.value());
        List<String> valueChecks = new ArrayList<>();

        if (value instanceof String) {
            valueChecks.add((String) value);
        } else if (value instanceof List && ((List) value).size() > 0 && ((List) value).get(0) instanceof String) {
            valueChecks = (List<String>) value;
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0 && ((Map) value).keySet().toArray()[0] instanceof String) {
            valueChecks = (List<String>) new ArrayList(Arrays.asList(((Map) value).keySet().toArray()));
        }

        if (!valueChecks.isEmpty()) {
            return valueChecks.stream().allMatch(o -> isValidString(o, validValues));
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.display().length == 0
            ? Arrays.toString(annotation.value()) : Arrays.toString(annotation.display()));
    }

    private boolean isValidString(String valueCheck, List<String> validValues) {
        return validValues.stream().anyMatch(valueCheck::matches);
    }
}
