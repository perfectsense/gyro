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
            double[] primaryFieldValues = annotation.selectedValues();
            String dependentField = annotation.dependent();
            double[] dependentFieldValues = annotation.dependentValues();

            try {
                Object primaryFieldValue = ValidationUtils.getValueFromField(primaryFieldName, diffable);

                if (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED)) {
                    if (isValidNumber(primaryFieldValue)
                        && (primaryFieldValues.length == 0
                        || Arrays.stream(primaryFieldValues).anyMatch(o -> o == getFieldValueNumber(primaryFieldValue)))) {

                        Object dependentFieldValue = ValidationUtils.getValueFromField(dependentField, diffable);

                        if ((dependentFieldValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                            || isValidNumber(dependentFieldValue)
                            && Arrays.stream(dependentFieldValues).noneMatch(o -> o == getFieldValueNumber(dependentFieldValue))) {
                            isValid = false;
                        }
                    }
                } else {
                    if (primaryFieldValue == null
                        || ((isValidNumber(primaryFieldValue)
                        && (primaryFieldValues.length > 0)
                        && (Arrays.stream(primaryFieldValues).noneMatch(o -> o == getFieldValueNumber(primaryFieldValue)))))) {

                        Object dependentFieldValue = ValidationUtils.getValueFromField(dependentField, diffable);

                        if ((dependentFieldValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                            || isValidNumber(dependentFieldValue)
                            && Arrays.stream(dependentFieldValues).anyMatch(o -> o == getFieldValueNumber(dependentFieldValue))) {
                            isValid = false;
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
            ValidationUtils.getFieldName(annotation.dependent()),
            (annotation.dependentValues().length == 0 ? "" : " with values "
                + (!annotation.isDependentDouble()
                ? Arrays.toString(Arrays.stream(annotation.dependentValues()).mapToLong(o -> (long) o).toArray())
                : Arrays.toString(annotation.dependentValues()))),
            (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.selected()),
            (annotation.selectedValues().length == 0 ? "" : " to "
                + (!annotation.isSelectedDouble()
                ? Arrays.toString(Arrays.stream(annotation.selectedValues()).mapToLong(o -> (long) o).toArray())
                : Arrays.toString(annotation.selectedValues())))
        );
    }

    private Double getFieldValueNumber(Object value) {
        if (value != null) {
            return ValidationUtils.getDoubleValue(value);
        } else {
            return null;
        }
    }

    private boolean isValidNumber(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Double;
    }
}
