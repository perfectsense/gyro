package gyro.core.validation;

import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConflictsWithValidator implements Validator<ConflictsWith> {
    @Override
    public boolean isValid(Diffable diffable, ConflictsWith annotation, Object value) {
        DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);

        return  (isValueEmpty(value) || Stream.of(annotation.value()).allMatch(o -> isFieldEmpty(o, diffableType, diffable)));
    }

    @Override
    public String getMessage(ConflictsWith annotation) {
        return String.format("Cannot be set when any of '%s' is set."
            , String.join("', '", annotation.value()));
    }

    private boolean isFieldEmpty(String name, DiffableType<Diffable> diffableType, Diffable diffable) {
        DiffableField field = diffableType.getField(name);

        return field == null || isValueEmpty(field.getValue(diffable));
    }

    private boolean isValueEmpty(Object value) {
        return  (value == null)
            || ((value instanceof List) && ((List<?>) value).isEmpty())
            || ((value instanceof Map) && ((Map<?, ?>) value).isEmpty());
    }
}
