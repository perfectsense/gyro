package gyro.core.validation;

import java.util.Arrays;

public class AllowedRegexValidator extends AnnotationBaseProcessor<AllowedRegex> {
    private static AllowedRegexValidator constructor = new AllowedRegexValidator();

    private AllowedRegexValidator() {
    }

    public static AnnotationProcessor getAnnotationProcessor() {
        return constructor;
    }

    @Override
    boolean doValidation(Object value) {
        String valueCheck = (String) value;

        for (String regex : annotation.value()) {
            if (valueCheck.matches(regex)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), annotation.display().length == 0
            ? Arrays.toString(annotation.value()) : Arrays.toString(annotation.display()));
    }
}
