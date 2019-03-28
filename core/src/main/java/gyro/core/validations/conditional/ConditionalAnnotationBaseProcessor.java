package gyro.core.validations.conditional;

import java.lang.annotation.Annotation;

import gyro.core.validations.AnnotationProcessor;
import gyro.core.validations.ValidationUtils;
import gyro.core.validations.ValidationUtils.DependencyType;
import gyro.core.validations.ValidationUtils.FieldType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ConditionalAnnotationBaseProcessor<A extends Annotation> implements AnnotationProcessor<A> {
    protected A annotation;

    protected abstract boolean doValidation(Object value);

    protected abstract Object getSelectedValues();

    protected abstract Object getDependentValues();

    @Override
    public void initialize(A annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return true;
        }

        return doValidation(value);
    }

    boolean selectedValidation(Object selectedFieldValue, DependencyType dependencyType, FieldType fieldType) {
        if (fieldType.equals(FieldType.STRING)) {
            String[] selectedValidValues = (String[]) getSelectedValues();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return (selectedFieldValue instanceof String
                    && (selectedValidValues.length == 0 || Arrays.asList(selectedValidValues).contains(selectedFieldValue)));
            } else {
                return (!ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    || ((selectedFieldValue instanceof String)
                    && (selectedValidValues.length > 0)
                    && (!Arrays.asList(selectedValidValues).contains(selectedFieldValue))));
            }
        } else if (fieldType.equals(FieldType.STRINGLIST)) {
            String[] selectedValidValues = (String[]) getSelectedValues();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return  ((selectedFieldValue instanceof List
                    && !((List) selectedFieldValue).isEmpty()
                    && ((List) selectedFieldValue).get(0) instanceof String)
                    && (selectedValidValues.length == 0 || Arrays.asList(selectedValidValues).containsAll((List<String>) selectedFieldValue)));
            } else {
                return (!ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    || ((selectedFieldValue instanceof List
                    && !((List) selectedFieldValue).isEmpty()
                    && ((List) selectedFieldValue).get(0) instanceof String)
                    && (selectedValidValues.length > 0)
                    && (!Arrays.asList(selectedValidValues).containsAll((List<String>) selectedFieldValue))));
            }
        } else if (fieldType.equals(FieldType.STRINGMAP)) {
            String[] selectedValidValues = (String[]) getSelectedValues();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return ((selectedFieldValue instanceof Map
                    && !((Map) selectedFieldValue).isEmpty()
                    && ((Map) selectedFieldValue).keySet().toArray()[0] instanceof String)
                    && (selectedValidValues.length == 0
                    || Arrays.asList(selectedValidValues).containsAll((Set<String>) ((Map) selectedFieldValue).keySet())));
            } else {
                return (!ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    || ((selectedFieldValue instanceof Map
                    && !((Map) selectedFieldValue).isEmpty()
                    && ((Map) selectedFieldValue).keySet().toArray()[0] instanceof String)
                    && (selectedValidValues.length > 0)
                    && (!Arrays.asList(selectedValidValues).containsAll((Set<String>) ((Map) selectedFieldValue).keySet()))));
            }
        } else if (fieldType.equals(FieldType.NUMBER)) {
            double[] selectedValues = (double[]) getSelectedValues();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return (isValidNumber(selectedFieldValue)
                    && (selectedValues.length == 0
                    || Arrays.stream(selectedValues).anyMatch(o -> o == getFieldValueNumber(selectedFieldValue))));
            } else {
                return (selectedFieldValue == null
                    || ((isValidNumber(selectedFieldValue)
                    && (selectedValues.length > 0)
                    && (Arrays.stream(selectedValues).noneMatch(o -> o == getFieldValueNumber(selectedFieldValue))))));
            }
        } else if (fieldType.equals(FieldType.NUMBERLIST)) {
            double[] selectedValues = (double[]) getSelectedValues();

            HashSet<Double> sourceValueSet = ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                && selectedFieldValue instanceof List
                && ((List) selectedFieldValue).get(0) instanceof Number
                ? new HashSet<>((List<Double>) selectedFieldValue) : new HashSet<>();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return (ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    && selectedFieldValue instanceof List
                    && ((List) selectedFieldValue).get(0) instanceof Number
                    && (selectedValues.length == 0
                    || Arrays.stream(selectedValues).allMatch(sourceValueSet::contains)));
            } else {
                return (selectedFieldValue == null
                    || (ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    && selectedFieldValue instanceof List
                    && ((List) selectedFieldValue).get(0) instanceof Number
                    && (selectedValues.length > 0)
                    && !(Arrays.stream(selectedValues).allMatch(sourceValueSet::contains))));
            }
        } else if (fieldType.equals(FieldType.NUMBERMAP)) {
            double[] selectedValues = (double[]) getSelectedValues();

            HashSet<Double> sourceValueSet = ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                && selectedFieldValue instanceof Map
                && ((Map) selectedFieldValue).keySet().toArray()[0] instanceof Number
                ? new HashSet<>(((Map) selectedFieldValue).keySet()) : new HashSet<>();

            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return (ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    && selectedFieldValue instanceof Map
                    && ((Map) selectedFieldValue).keySet().toArray()[0] instanceof Number
                    && (selectedValues.length == 0
                    || Arrays.stream(selectedValues).allMatch(sourceValueSet::contains)));
            } else {
                return (selectedFieldValue == null
                    || (ValidationUtils.isNotNullOrEmpty(selectedFieldValue)
                    && selectedFieldValue instanceof Map
                    && ((Map) selectedFieldValue).keySet().toArray()[0] instanceof Number
                    && (selectedValues.length > 0)
                    && !(Arrays.stream(selectedValues).allMatch(sourceValueSet::contains))));
            }
        } else if (fieldType.equals(FieldType.BOOLEAN)) {
            boolean selectedValidValue = (boolean) getSelectedValues();
            if (dependencyType.equals(ValidationUtils.DependencyType.REQUIRED)) {
                return (selectedFieldValue instanceof Boolean && ((Boolean) selectedFieldValue == selectedValidValue));
            } else {
                return (!(selectedFieldValue instanceof Boolean)
                    || ((Boolean) selectedFieldValue != selectedValidValue));
            }
        } else {
            return (dependencyType.equals(DependencyType.REQUIRED)) == ValidationUtils.isNotNullOrEmpty(selectedFieldValue);
        }
    }

    boolean dependentValidation(Object dependentFieldValue, DependencyType dependencyType, FieldType fieldType) {
        if (fieldType.equals(FieldType.STRING)) {
            String[] dependentValues = (String[]) getDependentValues();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof String && !Arrays.asList(dependentValues).contains(dependentFieldValue));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof String && Arrays.asList(dependentValues).contains(dependentFieldValue));
            }
        } else if (fieldType.equals(FieldType.STRINGLIST)) {
            String[] dependentValues = (String[]) getDependentValues();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof List
                    && ((List) dependentFieldValue).get(0) instanceof String
                    && !Arrays.asList(dependentValues).containsAll((List<String>) dependentFieldValue));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof List
                    && ((List) dependentFieldValue).get(0) instanceof String
                    && Arrays.asList(dependentValues).containsAll((List<String>) dependentFieldValue));
            }
        } else if (fieldType.equals(FieldType.STRINGMAP)) {
            String[] dependentValues = (String[]) getDependentValues();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof Map
                    && ((Map) dependentFieldValue).keySet().toArray()[0] instanceof String
                    && !Arrays.asList(dependentValues).containsAll((Set<String>) ((Map) dependentFieldValue).keySet()));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof Map
                    && ((Map) dependentFieldValue).keySet().toArray()[0] instanceof String
                    && Arrays.asList(dependentValues).containsAll((Set<String>) ((Map) dependentFieldValue).keySet()));
            }
        } else if (fieldType.equals(FieldType.NUMBER)) {
            double[] dependentValues = (double[]) getDependentValues();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || isValidNumber(dependentFieldValue)
                    && Arrays.stream(dependentValues).noneMatch(o -> o == getFieldValueNumber(dependentFieldValue)));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || isValidNumber(dependentFieldValue)
                    && Arrays.stream(dependentValues).anyMatch(o -> o == getFieldValueNumber(dependentFieldValue)));
            }
        } else if (fieldType.equals(FieldType.NUMBERLIST)) {
            double[] dependentValues = (double[]) getDependentValues();

            HashSet<Double> dependentValueSet = ValidationUtils.isNotNullOrEmpty(dependentFieldValue)
                && dependentFieldValue instanceof List
                && ((List) dependentFieldValue).get(0) instanceof Number
                ? new HashSet<>((List<Double>) dependentFieldValue) : new HashSet<>();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof List
                    && ((List) dependentFieldValue).get(0) instanceof Number
                    && Arrays.stream(dependentValues).noneMatch(dependentValueSet::contains));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof List
                    && ((List) dependentFieldValue).get(0) instanceof Number
                    && Arrays.stream(dependentValues).anyMatch(dependentValueSet::contains));
            }
        } else if (fieldType.equals(FieldType.NUMBERMAP)) {
            double[] dependentValues = (double[]) getDependentValues();

            HashSet<Double> dependentValueSet = ValidationUtils.isNotNullOrEmpty(dependentFieldValue)
                && dependentFieldValue instanceof Map
                && ((Map) dependentFieldValue).keySet().toArray()[0] instanceof Number
                ? new HashSet<>(((Map) dependentFieldValue).keySet()) : new HashSet<>();

            if (dependencyType.equals(DependencyType.REQUIRED)) {
                return ((dependentValues.length == 0 && !ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof Map
                    && ((Map) dependentFieldValue).keySet().toArray()[0] instanceof Number
                    && Arrays.stream(dependentValues).noneMatch(dependentValueSet::contains));
            } else {
                return ((dependentValues.length == 0 && ValidationUtils.isNotNullOrEmpty(dependentFieldValue))
                    || dependentFieldValue instanceof Map
                    && ((Map) dependentFieldValue).keySet().toArray()[0] instanceof Number
                    && Arrays.stream(dependentValues).anyMatch(dependentValueSet::contains));
            }
        } else if (fieldType.equals(FieldType.BOOLEAN)) {
            if (dependentFieldValue instanceof Boolean) {
                boolean dependentValue = (boolean) getDependentValues();
                return dependencyType.equals(ValidationUtils.DependencyType.REQUIRED) == (dependentValue == (Boolean) dependentFieldValue);
            } else {
                return true;
            }
        } else {
            return dependencyType.equals(ValidationUtils.DependencyType.REQUIRED) == ValidationUtils.isNotNullOrEmpty(dependentFieldValue);
        }
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
