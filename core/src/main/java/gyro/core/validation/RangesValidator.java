package gyro.core.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RangesValidator extends AbstractRepeatableValidator<Ranges> {
    private static RangesValidator constructor = new RangesValidator();

    private RangesValidator() {

    }

    public static RepeatableValidator getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        Validator validator = RangeValidator.getAnnotationProcessor();

        List<String> rangesString = new ArrayList<>();

        List<Object> values = new ArrayList<>();

        if (value instanceof Number) {
            values.add(value);
        } else if (value instanceof List) {
            values.addAll(((List) value));
        } else if (value instanceof Map) {
            values.addAll(((Map) value).keySet());
        }

        for (Object val : values) {
            rangesString = new ArrayList<>();

            for (Range range : annotation.value()) {
                if (!validator.isValid(range, val)) {
                    if (((RangeValidator) validator).isDouble) {
                        rangesString.add(String.format("[%s - %s]",
                            range.low(),
                            range.high()));
                    } else {
                        rangesString.add(String.format("[%s - %s]",
                            (long) range.low(),
                            (long) range.high()));
                    }
                } else {
                    break;
                }
            }

            if (rangesString.size() == annotation.value().length) {
                break;
            }
        }

        if (rangesString.size() == annotation.value().length) {
            validationMessages.add(String.format("Valid number should be in one of these ranges %s.", String.join(", ", rangesString)));
        }

        return validationMessages;
    }
}
