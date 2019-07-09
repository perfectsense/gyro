package gyro.core.workflow;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class DeleteDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = node.getArguments();

        if (arguments.size() != 1) {
            throw new GyroException("@delete directives only takes 1 argument!");
        }

        scope.getSettings(WorkflowSettings.class)
            .getActions()
            .add(new DeleteAction(arguments.get(0)));
    }

}
