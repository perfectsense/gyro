package gyro.core.reference;

import java.util.List;

import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;

public abstract class ReferenceResolver {

    public static Object resolveRemaining(Scope scope, List<Object> arguments, Object value) {
        if (value == null) {
            return null;
        }

        if (arguments != null) {
            for (Object argument : arguments) {
                value = NodeEvaluator.getValue(value, (String) argument);

                if (value == null) {
                    return null;
                }
            }
        }

        return value;
    }

    public abstract String getName();

    public abstract Object resolve(Scope scope, List<Object> arguments) throws Exception;

}
