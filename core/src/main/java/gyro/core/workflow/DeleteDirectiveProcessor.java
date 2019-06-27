package gyro.core.workflow;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class DeleteDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Node> arguments = node.getArguments();

        if (arguments.size() != 2) {
            throw new GyroException("@delete directives only takes 2 arguments!");
        }

        scope.getSettings(WorkflowSettings.class)
            .getDeletes()
            .add(new Delete(arguments.get(0), arguments.get(1)));
    }

}
