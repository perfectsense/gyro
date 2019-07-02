package gyro.core.workflow;

import com.google.common.base.Preconditions;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.State;
import gyro.lang.ast.Node;

public class SwapAction extends Action {

    private final Node x;
    private final Node y;

    public SwapAction(Node x, Node y) {
        this.x = Preconditions.checkNotNull(x);
        this.y = Preconditions.checkNotNull(y);
    }

    public Node getX() {
        return x;
    }

    public Node getY() {
        return y;
    }

    @Override
    public void execute(GyroUI ui, State state, RootScope pending, Scope scope) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Object x = evaluator.visit(this.x, scope);

        if (x == null) {
            throw new GyroException("Can't swap because the first argument is null!");
        }

        if (!(x instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't swap the first argument [%s] because it's not a resource!",
                x));
        }

        Object y = evaluator.visit(this.y, scope);

        if (y == null) {
            throw new GyroException("Can't swap because the second argument is null!");
        }

        if (!(y instanceof Resource)) {
            throw new GyroException(String.format(
                "Can't swap the second argument [%s] because it's not a resource!",
                y));
        }

        Resource xResource = (Resource) x;
        Resource yResource = (Resource) y;

        ui.write("@|magenta ⤢ Swapping %s with %s|@\n", xResource.name(), yResource.name());
        state.swap(xResource, yResource);
    }

}
