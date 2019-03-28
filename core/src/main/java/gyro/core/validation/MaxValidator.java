package gyro.core.validation;

public class MaxValidator extends AnnotationBaseProcessor<Max> {
    private static MaxValidator constructor = new MaxValidator();

    private MaxValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        double valueCheck = ValidationUtils.getDoubleValue(value);
        double refValue = annotation.value();
        int result = Double.compare(valueCheck, refValue);

        return result <= 0;
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.isDouble()
            ? annotation.value()
            : (long) annotation.value());
    }
}
