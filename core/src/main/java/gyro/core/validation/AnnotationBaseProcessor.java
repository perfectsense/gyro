package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public abstract class AnnotationBaseProcessor<A extends Annotation> implements AnnotationProcessor<A> {
    A annotation;

    ValueType valueType;

    enum ValueType { SINGLE, LIST, MAP }

    abstract boolean doValidation(Object value);

    @Override
    public void initialize(A annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return true;
        }

        setValueType(value);

        return doValidation(value);
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
