package gyro.core.workflow;

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.scope.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class DeleteAction extends Action {

    private final Node resource;

    public DeleteAction(Node resource) {
        this.resource = resource;
    }

    public Node getResource() {
        return resource;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Object resource = evaluator.visit(this.resource, scope);

        if (resource == null) {
            throw new GyroException("Can't delete a null resource!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't delete @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                resource.getClass().getName()));
        }

        String fullName = ((Resource) resource).primaryKey();

        pending.getFileScopes().forEach(s -> s.remove(fullName));
    }

}
