package gyro.core.workflow;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class SwapDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "swap";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = node.getArguments();

        if (arguments.size() != 3) {
            throw new GyroException("@swap directives only takes 3 arguments!");
        }

        scope.getSettings(WorkflowSettings.class)
            .getSwaps()
            .add(new Swap(arguments.get(0), arguments.get(1), arguments.get(2)));
    }

}
