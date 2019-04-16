package gyro.core.validation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractStringValidator<A extends Annotation> extends AbstractValidator<A> {
    List<String> getValuesToCheck(Object value) {
        List<String> valueChecks = new ArrayList<>();

        if (value instanceof String) {
            valueChecks.add((String) value);
        } else if (value instanceof List && ((List) value).size() > 0 && ((List) value).get(0) instanceof String) {
            valueChecks = (List<String>) value;
        } else if (value instanceof Map && ((Map) value).keySet().size() > 0 && ((Map) value).keySet().toArray()[0] instanceof String) {
            valueChecks = (List<String>) new ArrayList(Arrays.asList(((Map) value).keySet().toArray()));
        }

        return valueChecks;
    }
}
