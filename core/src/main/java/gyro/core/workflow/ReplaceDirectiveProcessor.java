package gyro.core.workflow;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class ReplaceDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "replace";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = validateArguments(node, 2, 2);

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new ReplaceAction(arguments.get(0), arguments.get(1)));
    }

}
