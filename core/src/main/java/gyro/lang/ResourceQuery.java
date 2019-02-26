package gyro.lang;

import gyro.core.BeamException;
import gyro.core.diff.Diffable;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ResourceQuery<T extends Resource> extends Diffable {

    public abstract List<T> query(Map<String, String> filter);

    public abstract List<T> queryAll();

    public List<T> query() {
        System.out.println("calling query");
        return new ArrayList<>();
    }

    public void merge(ResourceQuery<Resource> other) {
        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();
            Object value = field.getValue(this);
            Object otherValue = field.getValue(other);
            if (value != null && otherValue != null) {
                throw new BeamException(String.format("%s is filtered more than once", key));
            } else if (otherValue != null) {
                field.setValue(this, otherValue);
            }
        }
    }

    @Override
    public String primaryKey() {
        return null;
    }

    @Override
    public String toDisplayString() {
        return null;
    }
}
/*

step 1: extract or

A | b or c and D | e and G or H

[A] | [b] or [c] and [D] | [e] and [G] or [H]

[A] | [b] or [c, D] | [e, G] or [H]

[A, b] or [A, c, D] | [e, G] or [H]

[A, b, e, G] or [A, b, H] or [A, c, D, e, G] or [A, c, D, H]

step 2: Locate api filter fields

[A, G] [b, e] or [A, H] [b] or [A, D, G] [c, e] or [A, D, H] [c]


*/