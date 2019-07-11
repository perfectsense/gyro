package gyro.core.resource;

public abstract class ResourceVisitor {

    public abstract void visit(String name, Scope scope);

}
