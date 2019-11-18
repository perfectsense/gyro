package gyro.core.validation;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConflictFieldsValidator extends AbstractDependencyValidator<ConflictFields> {

    private static final ConflictFieldValidator VALIDATOR = new ConflictFieldValidator();

    @Override
    public boolean isValid(ConflictFields annotation, Object value) {
        return Stream.of(annotation.value()).anyMatch(a -> VALIDATOR.isValid(a, value));
    }

    @Override
    public String getMessage(ConflictFields annotation) {
        return getMessage(annotation.value());
    }

    private String getMessage(ConflictField ... annotation) {
        return String.format("Cannot be set when any of '%s' is set."
            , Stream.of(annotation)
            .map(ConflictField::conflict)
            .collect(Collectors.joining("', '")));
    }
}
