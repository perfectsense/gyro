package gyro.core.validation;

import java.util.Arrays;

public class AllowedNumbersValidator extends AnnotationNumberBaseProcessor<AllowedNumbers> {
    private static AllowedNumbersValidator constructor = new AllowedNumbersValidator();

    private AllowedNumbersValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        double valueCheck = ValidationUtils.getDoubleValue(value);
        double[] refList = annotation.value();
        return Arrays.stream(refList).anyMatch(o -> o == valueCheck);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), isDouble
            ? Arrays.toString(annotation.value())
            : Arrays.toString(Arrays.stream(annotation.value()).mapToLong(o -> (long)o).toArray()));
    }
}
