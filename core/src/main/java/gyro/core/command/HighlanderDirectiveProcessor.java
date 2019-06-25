package gyro.core.command;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class HighlanderDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "highlander";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@highlander directive can only be used within the init.gyro file!");
        }

        List<Object> arguments = resolveArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@highlander directive only takes 1 argument!");
        }

        scope.getSettings(HighlanderSettings.class).setHighlander((boolean) arguments.get(0));
    }

}
