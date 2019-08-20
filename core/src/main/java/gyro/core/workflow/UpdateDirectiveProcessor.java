package gyro.core.workflow;

import java.util.List;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

@Type("update")
public class UpdateDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = validateArguments(node, 1, 1);

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new UpdateAction(arguments.get(0), node.getBody()));
    }

}
