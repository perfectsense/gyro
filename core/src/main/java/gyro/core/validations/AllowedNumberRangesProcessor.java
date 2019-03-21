package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class AllowedNumberRangesProcessor extends RepeatableAnnotationBaseProcessor<AllowedNumberRanges> {
    private static AllowedNumberRangesProcessor constructor = new AllowedNumberRangesProcessor();

    private AllowedNumberRangesProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = AllowedNumberRangeValidator.getAnnotationProcessor();

        List<String> rangesString = new ArrayList<>();

        for (AllowedNumberRange allowedNumberRange : annotation.value()) {
            annotationProcessor.initialize(allowedNumberRange);
            if (!annotationProcessor.isValid(value)) {
                if (allowedNumberRange.isDouble()) {
                    rangesString.add(String.format("[%s - %s]", allowedNumberRange.low(), allowedNumberRange.high()));
                } else {
                    rangesString.add(String.format("[%s - %s]", (long) allowedNumberRange.low(), (long) allowedNumberRange.high()));
                }
            }
        }

        if (rangesString.size() == annotation.value().length) {
            validationMessages.add(String.format(annotation.message(), String.join(", ", rangesString)));
        }

        return validationMessages;
    }
}
