package gyro.core.workflow;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.Node;

public class UpdateAction extends Action {

    private final Node resource;
    private final List<Node> body;

    public UpdateAction(Node resource, List<Node> body) {
        this.resource = Preconditions.checkNotNull(resource);
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public Node getResource() {
        return resource;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Object resource = evaluator.visit(this.resource, scope);

        if (resource == null) {
            throw new GyroException("Can't update because the resource is null!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't update [%s] because it's not a resource!",
                resource));
        }

        String fullName = ((Resource) resource).primaryKey();
        Resource pendingResource = pending.findResource(fullName);
        DiffableScope resourceScope = pendingResource.scope();

        for (Node item : body) {
            evaluator.visit(item, resourceScope);
        }

        pendingResource.initialize(resourceScope);
    }

}
