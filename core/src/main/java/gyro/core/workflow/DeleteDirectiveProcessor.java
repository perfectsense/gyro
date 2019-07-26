package gyro.core.workflow;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class DeleteDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = validateArguments(node, 1, 1);

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new DeleteAction(arguments.get(0)));
    }

}
