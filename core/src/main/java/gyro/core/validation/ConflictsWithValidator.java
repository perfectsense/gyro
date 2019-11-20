package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;

import java.util.stream.Stream;

public class ConflictsWithValidator implements Validator<ConflictsWith> {
    @Override
    public boolean isValid(Diffable diffable, ConflictsWith annotation, Object fieldValue) {
        DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);

        return  (ObjectUtils.isBlank(fieldValue) || Stream.of(annotation.value())
            .allMatch(name -> {
                DiffableField field = diffableType.getField(name);
                return field == null || ObjectUtils.isBlank(field.getValue(diffable));
            }));
    }

    @Override
    public String getMessage(ConflictsWith annotation) {
        return String.format("Cannot be set when any of the following field(s) are set: ['%s']"
            , String.join("', '", annotation.value()));
    }
}
