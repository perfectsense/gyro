package gyro.core.validation.conditional;

import gyro.core.diff.Diffable;
import gyro.core.validation.AnnotationProcessor;
import gyro.core.validation.ValidationUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ConditionalValidator extends ConditionalAnnotationBaseProcessor<Conditional> {
    private static ConditionalValidator constructor = new ConditionalValidator();

    private ConditionalValidator() {

    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    protected boolean doValidation(Object value) {
        boolean isValid = true;

        if (getSelectedValues() != null && getDependentValues() != null && value instanceof Diffable) {
            Diffable diffable = (Diffable) value;

            try {
                Object selectedFieldValue = ValidationUtils.getValueFromField(annotation.source(), diffable);

                if (selectedValidation(selectedFieldValue, annotation.dependencyType(), getSelectedFieldType())) {
                    Object dependentFieldValue = ValidationUtils.getValueFromField(annotation.dependent(), diffable);
                    isValid = dependentValidation(dependentFieldValue, annotation.dependencyType(), getDependentFieldType());
                }

            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

        return isValid;
    }

    @Override
    protected Object getSelectedValues() {
        if (!annotation.sourceStringValues().isDefault()) {
            return annotation.sourceStringValues().value();
        } else if (!annotation.sourceStringListValues().isDefault()) {
            return annotation.sourceStringListValues().value();
        } else if (!annotation.sourceStringMapValues().isDefault()) {
            return annotation.sourceStringMapValues().value();
        } else if (!annotation.sourceNumberValues().isDefault()) {
            return annotation.sourceNumberValues().value();
        } else if (!annotation.sourceNumberListValues().isDefault()) {
            return annotation.sourceNumberListValues().value();
        } else if (!annotation.sourceNumberMapValues().isDefault()) {
            return annotation.sourceNumberMapValues().value();
        } else if (!annotation.sourceBooleanValues().isDefault()) {
            return annotation.sourceBooleanValues().value();
        } else {
            return null;
        }
    }

    @Override
    protected Object getDependentValues() {
        if (!annotation.dependentStringValues().isDefault()) {
            return annotation.dependentStringValues().value();
        } else if (!annotation.dependentStringListValues().isDefault()) {
            return annotation.dependentStringListValues().value();
        } else if (!annotation.dependentStringMapValues().isDefault()) {
            return annotation.dependentStringMapValues().value();
        } else if (!annotation.dependentNumberValues().isDefault()) {
            return annotation.dependentNumberValues().value();
        } else if (!annotation.dependentNumberListValues().isDefault()) {
            return annotation.dependentNumberListValues().value();
        } else if (!annotation.dependentNumberMapValues().isDefault()) {
            return annotation.dependentNumberMapValues().value();
        } else if (!annotation.dependentBooleanValues().isDefault()) {
            return annotation.dependentBooleanValues().value();
        } else {
            return null;
        }
    }

    //@Override
    protected String getSelectedValuesDisplay() {
        if (!annotation.sourceStringValues().isDefault()) {
            return getDisplayStrings(annotation.sourceStringValues().value());
        } else if (!annotation.sourceStringListValues().isDefault()) {
            return getDisplayStrings(annotation.sourceStringListValues().value());
        } else if (!annotation.sourceStringMapValues().isDefault()) {
            return getDisplayStrings(annotation.sourceStringMapValues().value());
        } else if (!annotation.sourceNumberValues().isDefault()) {
            return getDisplayNumbers(annotation.sourceNumberValues().value(), annotation.sourceNumberValues().isDouble());
        } else if (!annotation.sourceNumberListValues().isDefault()) {
            return getDisplayNumbers(annotation.sourceNumberListValues().value(), annotation.sourceNumberListValues().isDouble());
        } else if (!annotation.sourceNumberMapValues().isDefault()) {
            return getDisplayNumbers(annotation.sourceNumberMapValues().value(), annotation.sourceNumberMapValues().isDouble());
        } else if (!annotation.sourceBooleanValues().isDefault()) {
            return annotation.sourceBooleanValues().value() ? "True" : "False";
        } else {
            return "";
        }
    }

    //@Override
    protected String getDependentValueDisplay() {
        if (!annotation.dependentStringValues().isDefault()) {
            return getDisplayStrings(annotation.dependentStringValues().value());
        } else if (!annotation.dependentStringListValues().isDefault()) {
            return getDisplayStrings(annotation.dependentStringListValues().value());
        } else if (!annotation.dependentStringMapValues().isDefault()) {
            return getDisplayStrings(annotation.dependentStringMapValues().value());
        } else if (!annotation.dependentNumberValues().isDefault()) {
            return getDisplayNumbers(annotation.dependentNumberValues().value(), annotation.dependentNumberValues().isDouble());
        } else if (!annotation.dependentNumberListValues().isDefault()) {
            return getDisplayNumbers(annotation.dependentNumberListValues().value(), annotation.dependentNumberListValues().isDouble());
        } else if (!annotation.dependentNumberMapValues().isDefault()) {
            return getDisplayNumbers(annotation.dependentNumberMapValues().value(), annotation.dependentNumberMapValues().isDouble());
        } else if (!annotation.dependentBooleanValues().isDefault()) {
            return annotation.dependentBooleanValues().value() ? "True" : "False";
        } else {
            return "";
        }
    }

    private ValidationUtils.FieldType getSelectedFieldType() {
        if (!annotation.sourceStringValues().isDefault()) {
            return ValidationUtils.FieldType.STRING;
        } else if (!annotation.sourceStringListValues().isDefault()) {
            return ValidationUtils.FieldType.STRINGLIST;
        } else if (!annotation.sourceStringMapValues().isDefault()) {
            return ValidationUtils.FieldType.STRINGMAP;
        } else if (!annotation.sourceNumberValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBER;
        } else if (!annotation.sourceNumberListValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBERLIST;
        } else if (!annotation.sourceNumberMapValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBERMAP;
        } else if (!annotation.sourceBooleanValues().isDefault()) {
            return ValidationUtils.FieldType.BOOLEAN;
        } else {
            return ValidationUtils.FieldType.FIELD;
        }
    }

    private ValidationUtils.FieldType getDependentFieldType() {
        if (!annotation.dependentStringValues().isDefault()) {
            return ValidationUtils.FieldType.STRING;
        } else if (!annotation.dependentStringListValues().isDefault()) {
            return ValidationUtils.FieldType.STRINGLIST;
        } else if (!annotation.dependentStringMapValues().isDefault()) {
            return ValidationUtils.FieldType.STRINGMAP;
        } else if (!annotation.dependentNumberValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBER;
        } else if (!annotation.dependentNumberListValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBERLIST;
        } else if (!annotation.dependentNumberMapValues().isDefault()) {
            return ValidationUtils.FieldType.NUMBERMAP;
        } else if (!annotation.dependentBooleanValues().isDefault()) {
            return ValidationUtils.FieldType.BOOLEAN;
        } else {
            return ValidationUtils.FieldType.FIELD;
        }
    }

    @Override
    public String getMessage() {
        String dependentValueDisplay = getDependentValueDisplay();
        String selectedValuesDisplay = getSelectedValuesDisplay();
        return String.format(annotation.message(),
            ValidationUtils.getFieldName(annotation.dependent()),
            ((dependentValueDisplay.equals("[]") || dependentValueDisplay.equals(""))
                ? "" : (dependentValueDisplay.split(",").length == 1 ? " with value " : " with values ")
                + dependentValueDisplay),
            (annotation.dependencyType().equals(ValidationUtils.DependencyType.REQUIRED) ? "Required" : "only Allowed"),
            ValidationUtils.getFieldName(annotation.source()),
            ((selectedValuesDisplay.equals("[]") || selectedValuesDisplay.equals(""))
                ? "" : " to one of " + selectedValuesDisplay)
        );
    }

    private String getDisplayStrings(String[] strings) {
        return Arrays.toString(strings);
    }

    private String getDisplayNumbers(double[] numbers, boolean isDouble) {
        return (!isDouble
            ? Arrays.toString(Arrays.stream(numbers).mapToLong(o -> (long) o).toArray())
            : Arrays.toString(numbers));
    }
}
