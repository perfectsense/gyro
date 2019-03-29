package gyro.core.validation;

import java.util.List;
import java.util.Map;

public class MaxValidator extends AnnotationNumberBaseProcessor<Max> {
    private static MaxValidator constructor = new MaxValidator();

    private MaxValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);

            return valueCheck <= annotation.value();
        } else if (value instanceof List && ((List) value).size() > 0
            && ((List) value).get(0) instanceof Number) {
            return ((List) value).stream().allMatch(
                o -> (ValidationUtils.getDoubleValue(o) <= annotation.value())
            );
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0
            && ((Map) value).keySet().toArray()[0] instanceof Number) {
            return ((Map) value).keySet().stream().allMatch(
                o -> ValidationUtils.getDoubleValue(o) <= annotation.value()
            );
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        if (isDouble) {
            return String.format(annotation.message(), annotation.value());
        } else {
            return String.format(annotation.message(), (long) annotation.value());
        }
    }
}
