package gyro.core.virtual;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.FileScope;
import gyro.lang.ast.block.DirectiveNode;

public class VirtualDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public String getName() {
        return "virtual";
    }

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        scope.getRootScope().put(
            getArgument(scope, node, String.class, 0),
            new VirtualResourceVisitor(scope, node.getBody()));
    }

}
