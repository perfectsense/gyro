package gyro.core.resource;

import gyro.util.Bug;

public class ModificationField extends DiffableField {

    private DiffableField originalDiffableField;

    public ModificationField(DiffableField originalDiffableField) {
        super(originalDiffableField);

        this.originalDiffableField = originalDiffableField;
    }

    @Override
    public Object getValue(Diffable diffable) {
        for (Modification<? extends Diffable> modification : DiffableInternals.getModifications(diffable)) {
            DiffableType<Modification<? extends Diffable>> modificationType = DiffableType.getInstance(modification);

            for (DiffableField field : modificationType.getFields()) {
                if (originalDiffableField == field) {
                    return super.getValue(modification);
                }
            }
        }

        DiffableType<Diffable> type = DiffableType.getInstance(diffable);
        String name = DiffableInternals.getName(diffable);

        throw new Bug(String.format("Unable to match modification field '%s' with modification instance on resource %s %s",
            getName(), type, name));
    }

    @Override
    public void setValue(Diffable diffable, Object value) {
        for (Modification<? extends Diffable> modification : DiffableInternals.getModifications(diffable)) {
            DiffableType<Modification<? extends Diffable>> modificationType = DiffableType.getInstance(modification);

            for (DiffableField field : modificationType.getFields()) {
                if (originalDiffableField == field) {
                    super.setValue(modification, value);
                    return;
                }
            }
        }

        DiffableType<Diffable> type = DiffableType.getInstance(diffable);
        String name = DiffableInternals.getName(diffable);

        throw new Bug(String.format("Unable to match modification field '%s' with modification instance on resource %s %s",
            getName(), type, name));
    }

}
