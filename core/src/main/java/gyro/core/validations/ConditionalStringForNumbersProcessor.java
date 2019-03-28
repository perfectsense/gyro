package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalStringForNumbersProcessor extends RepeatableAnnotationBaseProcessor<ConditionalStringForNumbers> {
    private static ConditionalStringForNumbersProcessor constructor = new ConditionalStringForNumbersProcessor();

    private ConditionalStringForNumbersProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalStringDependentValidator.getAnnotationProcessor();

        for (ConditionalStringForNumber conditionalStringForNumber : annotation.value()) {
            annotationProcessor.initialize(conditionalStringForNumber);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
