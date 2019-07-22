package gyro.core.workflow;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class CreateDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = validateDirectiveArguments(node, 2, 2);

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new CreateAction(arguments.get(0), arguments.get(1), node.getBody()));
    }

}
