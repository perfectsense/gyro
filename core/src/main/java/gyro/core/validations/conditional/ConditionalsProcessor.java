package gyro.core.validations.conditional;

import gyro.core.validations.AnnotationProcessor;
import gyro.core.validations.RepeatableAnnotationBaseProcessor;
import gyro.core.validations.RepeatableAnnotationProcessor;

import java.util.ArrayList;
import java.util.List;

public class ConditionalsProcessor extends RepeatableAnnotationBaseProcessor<Conditionals> {
    private static ConditionalsProcessor constructor = new ConditionalsProcessor();

    private ConditionalsProcessor() {

    }

    public static RepeatableAnnotationProcessor getRepeatableAnnotationProcessor() {
        return constructor;
    }

    @Override
    public List<String> getValidations(Object value) {
        List<String> validationMessages = new ArrayList<>();

        AnnotationProcessor annotationProcessor = ConditionalValidator.getAnnotationProcessor();

        for (Conditional conditional : annotation.value()) {
            annotationProcessor.initialize(conditional);
            if (!annotationProcessor.isValid(value)) {
                validationMessages.add(annotationProcessor.getMessage());
            }
        }

        return validationMessages;
    }
}
