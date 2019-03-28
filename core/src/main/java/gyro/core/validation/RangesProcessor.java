package gyro.core.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RangesProcessor extends RepeatableAnnotationBaseProcessor<Ranges> {
    private static RangesProcessor constructor = new RangesProcessor();

    private RangesProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = RangeValidator.getAnnotationProcessor();

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
                annotationProcessor.initialize(range);
                if (!annotationProcessor.isValid(val)) {
                    if (((RangeValidator) annotationProcessor).isDouble) {
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
            validationMessages.add(String.format(annotation.message(), String.join(", ", rangesString)));
        }

        return validationMessages;
    }
}
