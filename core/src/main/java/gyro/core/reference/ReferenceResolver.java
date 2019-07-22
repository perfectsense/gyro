package gyro.core.reference;

import java.util.List;

import gyro.core.scope.Scope;

public abstract class ReferenceResolver {

    public abstract String getName();

    public abstract Object resolve(Scope scope, List<Object> arguments) throws Exception;

}
