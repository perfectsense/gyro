package gyro.core.validations;

import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ConditionalNumberDependentValidator extends AnnotationBaseProcessor<ConditionalNumberDependent> {
    private static ConditionalNumberDependentValidator constructor = new ConditionalNumberDependentValidator();

    private ConditionalNumberDependentValidator() {
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
            double[] primaryFieldValues = annotation.values();
            String[] dependentFields = annotation.dependent();

            try {
                Object primaryFieldValue = ValidationUtils.getValueFromField(primaryFieldName, diffable);

                if (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED)) {
                    if (isValidNumber(primaryFieldValue)
                        && (primaryFieldValues.length == 0
                        || Arrays.stream(primaryFieldValues).anyMatch(o -> o == getFieldValueNumber(primaryFieldValue)))) {
                        for (String requiredField : dependentFields) {
                            if (!ValidationUtils.isNullOrEmpty(ValidationUtils.getValueFromField(requiredField, diffable))) {
                                isValid = false;
                                break;
                            }
                        }
                    }
                } else {
                    if (primaryFieldValue == null
                        || ((isValidNumber(primaryFieldValue)
                        && (primaryFieldValues.length > 0)
                        && (Arrays.stream(primaryFieldValues).noneMatch(o -> o == getFieldValueNumber(primaryFieldValue)))))) {
                        for (String allowedField: dependentFields) {
                            if (ValidationUtils.getValueFromField(allowedField, diffable) != null) {
                                isValid = false;
                                break;
                            }
                        }
                    }
                }

            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

        return isValid;
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(),
            (annotation.dependent().length == 1 ? "" : "s"),
            (annotation.dependent().length == 1
                ? ValidationUtils.getFieldName(annotation.dependent()[0])
                : Arrays.stream(annotation.dependent()).map(ValidationUtils::getFieldName).toArray()),
            (annotation.dependent().length == 1 ? "is" : "are"),
            (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.selected()),
            (annotation.values().length == 0 ? "" : " to " + Arrays.toString(annotation.values()))
        );
    }

    private Double getFieldValueNumber(Object value) {
        if (value != null) {
            return (value instanceof Integer ? (double) (Integer) value : value instanceof Long ? (double) (Long) value : (double) value);
        } else {
            return null;
        }
    }

    private boolean isValidNumber(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Double;
    }
}
