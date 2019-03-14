package gyro.core.validations;

import java.util.Arrays;

public class AllowedNumbersValidator extends AnnotationBaseProcessor<AllowedNumbers> {
    private static AllowedNumbersValidator constructor = new AllowedNumbersValidator();

    private AllowedNumbersValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        double valueCheck = value instanceof Integer ? (double) (Integer) value : value instanceof Long ? (double) (Long) value : (double) value;
        double[] refList = annotation.value();
        return Arrays.stream(refList).anyMatch(o -> o == valueCheck);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.isDouble()
            ? Arrays.toString(annotation.value())
            : Arrays.toString(Arrays.stream(annotation.value()).mapToLong(o -> (long)o).toArray()));
    }
}
