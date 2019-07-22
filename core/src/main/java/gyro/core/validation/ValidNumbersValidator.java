package gyro.core.validation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ValidNumbersValidator extends AbstractValidator<ValidNumbers> {

    @Override
    protected boolean validate(ValidNumbers annotation, Object value) {
        if (value instanceof Number) {
            double check = ((Number) value).doubleValue();
            return Arrays.stream(annotation.value()).anyMatch(v -> v == check);
        }

        return false;
    }

    @Override
    public String getMessage(ValidNumbers annotation) {
        return "Must be one of " + Arrays.stream(annotation.value())
            .mapToObj(v -> String.format("@|bold %s|@", v))
            .collect(Collectors.joining(", "));
    }

}
