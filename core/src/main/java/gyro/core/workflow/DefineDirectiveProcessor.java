package gyro.core.workflow;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.FileScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("define")
public class DefineDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        validateArguments(node, 2, 2);

        scope.getRootScope()
            .getSettings(WorkflowSettings.class)
            .getWorkflows()
            .add(new Workflow(
                getArgument(scope, node, String.class, 0),
                getArgument(scope, node, String.class, 1),
                evaluateBody(scope, node)));
    }

}
