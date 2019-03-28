package gyro.core.validation;

public class RangeValidator extends AnnotationNumberBaseProcessor<Range> {
    private static RangeValidator constructor = new RangeValidator();

    private RangeValidator() {
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
        if (isDouble) {
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
