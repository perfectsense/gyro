package gyro.core.validations;

import gyro.core.diff.Diffable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ConditionalStringForNumberValidator extends ConditionalAnnotationBaseProcessor<ConditionalStringForNumber> {
    private static ConditionalStringForNumberValidator constructor = new ConditionalStringForNumberValidator();

    private ConditionalStringForNumberValidator() {

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
        return annotation.dependentValues();
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
                    isValid = dependentValidation(dependentFieldValue, annotation.type(), ValidationUtils.FieldType.NUMBER);
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
            (annotation.dependentValues().length == 0 ? "" : (annotation.dependentValues().length == 1 ? " with value " : " with values ")
                + (!annotation.isDependentDouble()
                ? Arrays.toString(Arrays.stream(annotation.dependentValues()).mapToLong(o -> (long) o).toArray())
                : Arrays.toString(annotation.dependentValues()))),
            (annotation.type().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.selected()),
            (annotation.selectedValues().length == 0 ? "" : " to " + Arrays.toString(annotation.selectedValues()))
        );
    }
}
