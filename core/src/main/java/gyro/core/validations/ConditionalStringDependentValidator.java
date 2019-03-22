package gyro.core.validations;

import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ConditionalStringDependentValidator extends AnnotationBaseProcessor<ConditionalStringDependent> {
    private static ConditionalStringDependentValidator constructor = new ConditionalStringDependentValidator();

    private ConditionalStringDependentValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        boolean isValid = true;

        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;
            String primaryFieldName = annotation.selected();
            String[] primaryFieldValues = annotation.values();
            String dependentField = annotation.dependent();
            String[] dependentFieldValues = annotation.dependentValues();

            try {
                Object primaryFieldValue = ValidationUtils.getValueFromField(primaryFieldName, diffable);

                if (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED)) {
                    if (primaryFieldValue instanceof String
                        && (primaryFieldValues.length == 0 || Arrays.asList(primaryFieldValues).contains(primaryFieldValue))) {

                        Object dependentFieldValue = ValidationUtils.getValueFromField(dependentField, diffable);

                        if ((dependentFieldValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                            || dependentFieldValue instanceof String && !Arrays.asList(dependentFieldValues).contains(dependentFieldValue)) {
                                isValid = false;
                        }
                    }
                } else {
                    /*if (!ValidationUtils.isNotNullOrEmpty(primaryFieldValue)
                        || ((primaryFieldValue instanceof String)
                        && (primaryFieldValues.length > 0)
                        && (!Arrays.asList(primaryFieldValues).contains(primaryFieldValue)))) {
                        for (String allowedField: dependentFields) {
                            if (ValidationUtils.getValueFromField(allowedField, diffable) != null) {
                                isValid = false;
                                break;
                            }
                        }
                    }*/
                }

            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

        return isValid;
    }

    @Override
    public String getMessage() {
        /*return String.format(annotation.message(),
            (annotation.dependent().length == 1 ? "" : "s"),
            (annotation.dependent().length == 1
                ? ValidationUtils.getFieldName(annotation.dependent()[0])
                : Arrays.stream(annotation.dependent()).map(ValidationUtils::getFieldName).toArray()),
            (annotation.dependent().length == 1 ? "is" : "are"),
            (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.selected()),
            (annotation.values().length == 0 ? "" : " to " + Arrays.toString(annotation.values()))
        );*/
        return "";
    }
}
