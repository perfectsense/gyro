package gyro.lang;

import gyro.core.BeamException;
import gyro.core.query.QueryField;
import gyro.core.query.QueryType;

import java.util.List;

public abstract class ResourceQuery<R extends Resource> {

    private String fieldName;
    private String operator;
    private Object value;

    public ResourceQuery() {

    }

    public ResourceQuery(String fieldName, String operator, Object value) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
    }

    public String fieldName() {
        return fieldName;
    }

    public void fieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String operator() {
        return operator;
    }

    public void operator(String operator) {
        this.operator = operator;
    }

    public Object value() {
        return value;
    }

    public void value(Object value) {
        this.value = value;
    }

    public final boolean merge(ResourceQuery<? extends Resource> other) {
        if (external() && other.external()) {
            for (QueryField field : QueryType.getInstance(getClass()).getFields()) {
                String key = field.getBeamName();
                Object value = field.getValue(this);
                Object otherValue = field.getValue(other);

                if (value != null && otherValue != null) {
                    throw new BeamException(String.format("%s is filtered more than once", key));

                } else if (otherValue != null) {
                    field.setValue(this, otherValue);
                }
            }

            return true;
        }

        return false;
    }

    public abstract boolean external();

    public abstract List<R> query();

    public abstract List<R> filter(List<R> resources);
}
