package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalNumberDependentsProcessor extends RepeatableAnnotationBaseProcessor<ConditionalNumberDependents> {
    private static ConditionalNumberDependentsProcessor constructor = new ConditionalNumberDependentsProcessor();

    private ConditionalNumberDependentsProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalNumberDependentValidator.getAnnotationProcessor();

        for (ConditionalNumberDependent conditionalNumberDependent : annotation.value()) {
            annotationProcessor.initialize(conditionalNumberDependent);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
