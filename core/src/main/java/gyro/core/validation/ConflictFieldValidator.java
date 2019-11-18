package gyro.core.validation;

import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;

import java.util.List;
import java.util.Map;

public class ConflictFieldValidator extends AbstractDependencyValidator<ConflictField> {
    @Override
    public boolean isValid(ConflictField annotation, Object value) {
        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;
            DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);

            return isFieldEmpty(annotation.source(), diffableType, diffable) || isFieldEmpty(annotation.conflict(), diffableType, diffable);
        }

        return false;
    }

    @Override
    public String getMessage(ConflictField annotation) {
        return String.format("Cannot be set when '%s' is set.", annotation.conflict());
    }

    private boolean isFieldEmpty(String name, DiffableType<Diffable> diffableType, Diffable diffable) {
        DiffableField field = diffableType.getField(name);

        if (field != null) {
            Object value = field.getValue(diffable);

            return  (value == null)
                || ((value instanceof List) && ((List<?>) value).isEmpty())
                || ((value instanceof Map) && ((Map<?, ?>) value).isEmpty());
        }

        return true;
    }
}
