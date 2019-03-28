package gyro.core.validations;

import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ConditionalStringForBooleanValidator extends ConditionalAnnotationBaseProcessor<ConditionalStringForBoolean> {
    private static ConditionalStringForBooleanValidator constructor = new ConditionalStringForBooleanValidator();

    private ConditionalStringForBooleanValidator() {

    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    protected Object getSelectedValues() {
        return annotation.selectedValues();
    }

    @Override
    protected Object getDependentValues() {
        return annotation.dependentValue();
    }

    @Override
    protected boolean doValidation(Object value) {
        boolean isValid = true;

        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;

            try {
                Object selectedFieldValue = ValidationUtils.getValueFromField(annotation.selected(), diffable);

                if (selectedValidation(selectedFieldValue, annotation.type(), ValidationUtils.FieldType.STRING)) {
                    Object dependentFieldValue = ValidationUtils.getValueFromField(annotation.dependent(), diffable);
                    isValid = dependentValidation(dependentFieldValue, annotation.type(), ValidationUtils.FieldType.BOOLEAN);
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
            " with value " + annotation.dependentValue(),
            (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.selected()),
            (annotation.selectedValues().length == 0 ? "" : " to " + Arrays.toString(annotation.selectedValues()))
        );
    }
}
