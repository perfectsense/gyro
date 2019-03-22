package gyro.core.validations;

public class AllowedNumberRangeValidator extends AnnotationBaseProcessor<AllowedNumberRange> {
    private static AllowedNumberRangeValidator constructor = new AllowedNumberRangeValidator();

    private AllowedNumberRangeValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        double valueCheck = ValidationUtils.getDoubleValue(value);

        return valueCheck >= annotation.low() && valueCheck <= annotation.high();
    }

    @Override
    public String getMessage() {
        if (annotation.isDouble()) {
            return String.format(annotation.message(),
                annotation.low(),
                annotation.high());
        } else {
            return String.format(annotation.message(),
                (long) annotation.low(),
                (long) annotation.high());
        }
    }
}
