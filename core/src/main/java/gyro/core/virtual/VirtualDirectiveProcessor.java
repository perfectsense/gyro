package gyro.core.virtual;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.FileScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class VirtualDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "virtual";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        if (!(scope instanceof FileScope)) {
            throw new GyroException("@virtual directive can only be used at root level of a gyro file!");
        }

        List<Object> arguments = evaluateArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@virtual directive only takes 1 argument!");
        }

        scope.getRootScope().put(
            (String) arguments.get(0),
            new VirtualResourceVisitor(scope, node.getBody()));
    }

}
