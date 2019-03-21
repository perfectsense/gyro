package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalStringDependentsProcessor extends RepeatableAnnotationBaseProcessor<ConditionalStringDependents> {
    private static ConditionalStringDependentsProcessor constructor = new ConditionalStringDependentsProcessor();

    private ConditionalStringDependentsProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalStringDependentValidator.getAnnotationProcessor();

        for (ConditionalStringDependent conditionalStringDependent : annotation.value()) {
            annotationProcessor.initialize(conditionalStringDependent);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
