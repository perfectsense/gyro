package gyro.core.workflow;

import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;

public class Delete {

    private final Node resource;

    public Delete(Node resource) {
        this.resource = resource;
    }

    public Node getResource() {
        return resource;
    }

    public void execute(GyroUI ui, RootScope currentRootScope, RootScope pendingRootScope, Scope executeScope) {
        Object resource = executeScope.getRootScope()
            .getEvaluator()
            .visit(this.resource, executeScope);

        if (resource == null) {
            throw new GyroException("Can't delete because the resource is null!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't delete [%s] because it's not a resource!",
                resource));
        }

        String fullName = ((Resource) resource).primaryKey();

        pendingRootScope.getFileScopes().forEach(s -> s.remove(fullName));
    }

}
