package gyro.core.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AllowedNumbersValidator extends AnnotationNumberBaseProcessor<AllowedNumbers> {
    private static AllowedNumbersValidator constructor = new AllowedNumbersValidator();

    private AllowedNumbersValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        HashSet<Double> doubles = new HashSet(Arrays.asList(annotation.value()));
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);
            return doubles.contains(valueCheck);
        } else if (value instanceof List && ((List) value).size() > 0) {
            return ((List) value).stream().map(ValidationUtils::getDoubleValue).allMatch(doubles::contains);
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0) {
            return ((Map) value).keySet().stream().map(ValidationUtils::getDoubleValue).allMatch(doubles::contains);
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), isDouble
            ? Arrays.toString(annotation.value())
            : Arrays.toString(Arrays.stream(annotation.value()).mapToLong(o -> (long)o).toArray()));
    }
}
