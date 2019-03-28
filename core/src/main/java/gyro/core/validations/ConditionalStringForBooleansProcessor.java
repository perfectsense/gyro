package gyro.core.validations;

import java.util.ArrayList;
import java.util.List;

public class ConditionalStringForBooleansProcessor extends RepeatableAnnotationBaseProcessor<ConditionalStringForBooleans> {
    private static ConditionalStringForBooleansProcessor constructor = new ConditionalStringForBooleansProcessor();

    private ConditionalStringForBooleansProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalStringDependentValidator.getAnnotationProcessor();

        for (ConditionalStringForBoolean conditionalStringForBoolean : annotation.value()) {
            annotationProcessor.initialize(conditionalStringForBoolean);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
