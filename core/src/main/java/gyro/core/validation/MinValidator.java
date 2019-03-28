package gyro.core.validation;

public class MinValidator extends AnnotationNumberBaseProcessor<Min> {
    private static MinValidator constructor = new MinValidator();

    private MinValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        double valueCheck = ValidationUtils.getDoubleValue(value);
        double refValue = annotation.value();
        int result = Double.compare(valueCheck, refValue);

        return result > 0;
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), isDouble
            ? annotation.value()
            : (long) annotation.value());
    }
}
