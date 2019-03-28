package gyro.core.validation;

public class AllowedMaxNumberValidator extends AnnotationBaseProcessor<AllowedMaxNumber> {
    private static AllowedMaxNumberValidator constructor = new AllowedMaxNumberValidator();

    private AllowedMaxNumberValidator() {
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
