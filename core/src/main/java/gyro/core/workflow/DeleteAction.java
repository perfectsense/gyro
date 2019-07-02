package gyro.core.workflow;

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
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
            throw new GyroException("Can't delete because the resource is null!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't delete [%s] because it's not a resource!",
                resource));
        }

        String fullName = ((Resource) resource).primaryKey();

        pending.removeResource(fullName);
    }

}
