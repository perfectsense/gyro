package gyro.core.validation;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class RegexesValidator extends AbstractValidator<Regexes> {

    private static final RegexValidator VALIDATOR = new RegexValidator();

    @Override
    protected boolean validate(Regexes annotation, Object value) {
        return Stream.of(annotation.value()).anyMatch(a -> VALIDATOR.validate(a, value));
    }

    String getMessage(Regex... annotations) {
        return "Must match " + Stream.of(annotations)
            .map(a -> {
                String message = a.message();
                return StringUtils.isBlank(message)
                    ? String.format("@|bold %s|@", a.value())
                    : message;
            })
            .collect(Collectors.joining(", or "));
    }

    @Override
    public String getMessage(Regexes annotation) {
        return getMessage(annotation.value());
    }

}
