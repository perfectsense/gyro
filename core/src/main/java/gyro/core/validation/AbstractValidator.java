package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidator<A extends Annotation> implements Validator<A> {
    A annotation;

    ValueType valueType;

    enum ValueType { SINGLE, LIST, MAP }

    abstract boolean validate(Object value);

    @Override
    public boolean isValid(A annotation, Object value) {
        this.annotation = annotation;

        if (value == null) {
            return true;
        }

        setValueType(value);

        return validate(value);
    }

    void setValueType(Object value) {
        if (value instanceof List) {
            valueType = ValueType.LIST;
        } else if (value instanceof Map) {
            valueType = ValueType.MAP;
        } else {
            valueType = ValueType.SINGLE;
        }
    }
}
