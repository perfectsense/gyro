package gyro.core.workflow;

import java.util.List;

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
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 2, 2);

        scope.getRootScope()
            .getSettings(WorkflowSettings.class)
            .getWorkflows()
            .add(new Workflow(
                (String) arguments.get(0),
                (String) arguments.get(1),
                evaluateBody(scope, node)));
    }

}
