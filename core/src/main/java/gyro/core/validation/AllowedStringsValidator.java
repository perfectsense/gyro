package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AllowedStringsValidator extends AbstractStringValidator<AllowedStrings> {
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
        return String.format("Valid value should be on of %s.",Arrays.toString(annotation.value()));
    }
}
