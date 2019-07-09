package gyro.core.command;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.lang.ast.block.DirectiveNode;

public class HighlanderDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "highlander";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@highlander directive only takes 1 argument!");
        }

        scope.getSettings(HighlanderSettings.class).setHighlander((boolean) arguments.get(0));
    }

}
