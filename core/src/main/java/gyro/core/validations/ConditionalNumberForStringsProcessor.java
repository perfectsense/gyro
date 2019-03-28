package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalNumberForStringsProcessor extends RepeatableAnnotationBaseProcessor<ConditionalNumberForStrings> {
    private static ConditionalNumberForStringsProcessor constructor = new ConditionalNumberForStringsProcessor();

    private ConditionalNumberForStringsProcessor() {

    }

    public static RepeatableAnnotationProcessor getreRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalStringDependentValidator.getAnnotationProcessor();

        for (ConditionalNumberForString conditionalNumberForString : annotation.value()) {
            annotationProcessor.initialize(conditionalNumberForString);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
