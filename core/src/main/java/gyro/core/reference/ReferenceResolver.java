package gyro.core.reference;

import java.util.List;

import gyro.core.resource.Scope;
import gyro.lang.query.Query;

public abstract class ReferenceResolver {

    public abstract String getName();

    public abstract Object resolve(Scope scope, List<Object> arguments, List<Query> queries) throws Exception;

}
