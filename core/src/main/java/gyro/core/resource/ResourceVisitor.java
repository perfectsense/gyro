package gyro.core.resource;

import gyro.core.scope.Scope;

public abstract class ResourceVisitor {

    public abstract void visit(String name, Scope scope);

}
