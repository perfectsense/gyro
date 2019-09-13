package gyro.core.resource;

import gyro.core.validation.ValidationError;

import java.util.Collections;
import java.util.List;

public class ModificationField extends DiffableField {

    public ModificationField(DiffableField field) {
        super(field);
    }

    @Override
    public Object getValue(Diffable diffable) {
        Diffable modification = DiffableInternals.getModifications(diffable).get(this);
        return super.getValue(modification);
    }

    @Override
    public void setValue(Diffable diffable, Object value) {
        Diffable modification = DiffableInternals.getModifications(diffable).get(this);
        super.setValue(modification, value);
    }

    @Override
    public List<ValidationError> validate(Diffable diffable) {
        return Collections.emptyList();
    }

}
