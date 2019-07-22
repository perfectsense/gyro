package gyro.core.validation;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangesValidator extends AbstractValidator<Ranges> {

    private static final RangeValidator VALIDATOR = new RangeValidator();

    @Override
    protected boolean validate(Ranges annotation, Object value) {
        return Stream.of(annotation.value()).anyMatch(a -> VALIDATOR.validate(a, value));
    }

    String getMessage(Range... annotations) {
        return "Must be between " + Stream.of(annotations)
            .map(a -> String.format("@|bold %s|@ and @|bold %s|@", a.min(), a.max()))
            .collect(Collectors.joining(", or "));
    }

    @Override
    public String getMessage(Ranges annotation) {
        return getMessage(annotation.value());
    }

}