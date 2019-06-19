package gyro.core.scope;

import java.util.List;

import gyro.core.reference.ReferenceResolver;
import gyro.core.resource.Scope;
import gyro.lang.query.Query;

class ConstantReferenceResolver extends ReferenceResolver {

    private final Object constant;

    public ConstantReferenceResolver(Object constant) {
        this.constant = constant;
    }

    @Override
    public String getName() {
        return "constant";
    }

    @Override
    public Object resolve(Scope scope, List<Object> arguments, List<Query> queries) {
        return constant;
    }

}
