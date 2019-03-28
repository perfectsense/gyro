package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalNumberForBooleansProcessor extends RepeatableAnnotationBaseProcessor<ConditionalNumberForBooleans> {
    private static ConditionalNumberForBooleansProcessor constructor = new ConditionalNumberForBooleansProcessor();

    private ConditionalNumberForBooleansProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalStringDependentValidator.getAnnotationProcessor();

        for (ConditionalNumberForBoolean conditionalNumberForBoolean : annotation.value()) {
            annotationProcessor.initialize(conditionalNumberForBoolean);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
