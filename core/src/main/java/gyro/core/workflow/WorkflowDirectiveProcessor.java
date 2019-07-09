package gyro.core.workflow;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.FileScope;
import gyro.lang.ast.block.DirectiveNode;

public class WorkflowDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public String getName() {
        return "workflow";
    }

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        if (node.getBody().isEmpty()) {
            throw new GyroException("@workflow directive requires a block!");
        }

        List<Object> arguments = evaluateArguments(scope, node);

        if (arguments.size() != 2) {
            throw new GyroException("@workflow directive only takes 2 arguments!");
        }

        scope.getRootScope()
            .getSettings(WorkflowSettings.class)
            .getWorkflows()
            .add(new Workflow(
                (String) arguments.get(0),
                (String) arguments.get(1),
                evaluateBody(scope, node)));
    }

}
