package gyro.core.workflow;

import com.google.common.base.Preconditions;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.scope.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class ReplaceAction extends Action {

    private final Node resource;
    private final Node with;

    public ReplaceAction(Node resource, Node with) {
        this.resource = Preconditions.checkNotNull(resource);
        this.with = Preconditions.checkNotNull(with);
    }

    public Node getResource() {
        return resource;
    }

    public Node getWith() {
        return with;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Object resource = evaluator.visit(this.resource, scope);

        if (resource == null) {
            throw new GyroException("Can't replace a null resource!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't replace @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                resource.getClass().getName()));
        }

        Object with = evaluator.visit(this.with, scope);

        if (with == null) {
            throw new GyroException(String.format(
                "Can't @|bold %s|@ resource with a null!",
                resource));
        }

        if (!(with instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't @|bold %s|@ resource with @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                with,
                with.getClass().getName()));
        }

        Resource resourceResource = (Resource) resource;
        Resource withResource = (Resource) with;
        String resourceKey = resourceResource.primaryKey();

        ui.write("@|magenta ⤢ Replacing %s with %s|@\n", resourceKey, withResource.primaryKey());
        state.replace(resourceResource, withResource);
        pending.getFileScopes().forEach(s -> s.remove(resourceKey));
    }

}
