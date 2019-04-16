package gyro.core.validation;

import java.util.Arrays;
import java.util.List;

public class AllowedRegexValidator extends AbstractStringValidator<AllowedRegex> {
    private static AllowedRegexValidator constructor = new AllowedRegexValidator();

    private AllowedRegexValidator() {
    }

    public static Validator getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean validate(Object value) {
        List<String> validValues = Arrays.asList(annotation.value());
        List<String> valueChecks = getValuesToCheck(value);

        if (!valueChecks.isEmpty()) {
            return valueChecks.stream().allMatch(o -> isValidString(o, validValues));
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format("Valid %s should be one of these formats %s.",
            valueType.equals(ValueType.MAP) ? "keys of the map" : (valueType.equals(ValueType.LIST) ? "strings" : "string"),
            annotation.display().length == 0 ? Arrays.toString(annotation.value()) : Arrays.toString(annotation.display()));
    }

    private boolean isValidString(String valueCheck, List<String> validValues) {
        return validValues.stream().anyMatch(valueCheck::matches);
    }
}
