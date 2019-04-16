package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AllowedStringsValidator extends AbstractStringValidator<AllowedStrings> {
    private static AllowedStringsValidator constructor = new AllowedStringsValidator();

    private AllowedStringsValidator() {
    }

    public static Validator getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean validate(Object value) {
        HashSet<String> validValues = new HashSet(Arrays.asList(annotation.value()));
        List<String> valueChecks = getValuesToCheck(value);

        if (!valueChecks.isEmpty()) {
            return validValues.containsAll(valueChecks);
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(),
            valueType.equals(ValueType.MAP) ? "keys of the map" : (valueType.equals(ValueType.LIST) ? "strings" : "string"),
            Arrays.toString(annotation.value()));
    }
}
