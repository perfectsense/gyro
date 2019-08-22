package gyro.core.scope;

import java.util.List;

import gyro.core.Type;
import gyro.core.reference.ReferenceResolver;

@Type("value")
public class ValueReferenceResolver extends ReferenceResolver {

    @Override
    public Object resolve(Scope scope, List<Object> arguments) {
        return arguments.isEmpty() ? null : arguments.get(0);
    }

}
