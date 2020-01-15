package gyro.core.validation;

import java.util.Set;
import java.util.stream.Stream;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;

public class ConflictsWithValidator implements Validator<ConflictsWith> {

    @Override
    public boolean isValid(Diffable diffable, ConflictsWith annotation, Object fieldValue) {
        if (ObjectUtils.isBlank(fieldValue)) {
            return true;
        }
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);
        DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);

        return Stream.of(annotation.value())
            .filter(name -> configuredFields.contains(name))
            .allMatch(name -> {
                DiffableField field = diffableType.getField(name);
                return field == null || ObjectUtils.isBlank(field.getValue(diffable));
            });
    }

    @Override
    public String getMessage(ConflictsWith annotation) {
        return String.format(
            "Cannot be set when any of the following field(s) are set: ['%s']",
            String.join("', '", annotation.value()));
    }
}
