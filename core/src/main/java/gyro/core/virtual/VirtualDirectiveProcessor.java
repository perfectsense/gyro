package gyro.core.virtual;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.FileScope;
import gyro.lang.ast.block.DirectiveNode;

public class VirtualDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public String getName() {
        return "virtual";
    }

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@virtual directive only takes 1 argument!");
        }

        scope.getRootScope().put(
            (String) arguments.get(0),
            new VirtualResourceVisitor(scope, node.getBody()));
    }

}
