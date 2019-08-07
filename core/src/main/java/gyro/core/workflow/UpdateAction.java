package gyro.core.workflow;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
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
            throw new GyroException("Can't update a null resource!");
        }

        if (!(resource instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't update @|bold %s|@, an instance of @|bold %s|@, because it's not a resource!",
                resource,
                resource.getClass().getName()));
        }

        String fullName = ((Resource) resource).primaryKey();
        Resource pendingResource = pending.findResource(fullName);
        DiffableScope resourceScope = DiffableInternals.getScope(pendingResource);

        for (Node item : body) {
            evaluator.visit(item, resourceScope);
        }

        DiffableType.getInstance(pendingResource).setValues(pendingResource, resourceScope);
    }

}
