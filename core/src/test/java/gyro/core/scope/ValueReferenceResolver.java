package gyro.core.scope;

import java.util.List;

import gyro.core.reference.ReferenceResolver;

public class ValueReferenceResolver extends ReferenceResolver {

    @Override
    public String getName() {
        return "value";
    }

    @Override
    public Object resolve(Scope scope, List<Object> arguments) {
        return arguments.isEmpty() ? null : arguments.get(0);
    }

}
