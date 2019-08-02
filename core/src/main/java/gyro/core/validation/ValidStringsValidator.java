package gyro.core.validation;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidStringsValidator extends AbstractValidator<ValidStrings> {

    @Override
    protected boolean validate(ValidStrings annotation, Object value) {
        return value instanceof String && Arrays.asList(annotation.value()).contains(value);
    }

    @Override
    public String getMessage(ValidStrings annotation) {
        return "Must be one of " + Stream.of(annotation.value())
            .map(v -> String.format("@|bold %s|@", v))
            .collect(Collectors.joining(", "));
    }

}
