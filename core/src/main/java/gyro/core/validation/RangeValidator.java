package gyro.core.validation;

import java.util.List;
import java.util.Map;

public class RangeValidator extends AbstractNumberValidator<Range> {
    private static RangeValidator constructor = new RangeValidator();

    private RangeValidator() {
    }

    public static Validator getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean validate(Object value) {
        if (value instanceof Number) {
            double valueCheck = ValidationUtils.getDoubleValue(value);

            return valueCheck >= annotation.low() && valueCheck <= annotation.high();
        } else if (value instanceof List && ((List) value).size() > 0
            && ((List) value).get(0) instanceof Number) {
            return ((List) value).stream().allMatch(
                o -> (ValidationUtils.getDoubleValue(o) >= annotation.low() && ValidationUtils.getDoubleValue(o) <= annotation.high())
            );
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0
            && ((Map) value).keySet().toArray()[0] instanceof Number) {
            return ((Map) value).keySet().stream().allMatch(
                o -> ValidationUtils.getDoubleValue(o) >= annotation.low() && ValidationUtils.getDoubleValue(o) <= annotation.high()
            );
        } else {
            return true;
        }
    }

    @Override
    public String getMessage() {
        if (isDouble) {
            return String.format(annotation.message(),
                valueType.equals(ValueType.MAP) ? "keys of the map" : (valueType.equals(ValueType.LIST) ? "numbers" : "number"),
                annotation.low(),
                annotation.high());
        } else {
            return String.format(annotation.message(),
                valueType.equals(ValueType.MAP) ? "keys of the map" : (valueType.equals(ValueType.LIST) ? "numbers" : "number"),
                (long) annotation.low(),
                (long) annotation.high());
        }
    }
}
