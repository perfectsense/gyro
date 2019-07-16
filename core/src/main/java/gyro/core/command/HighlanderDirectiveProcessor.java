package gyro.core.command;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;

public class HighlanderDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "highlander";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 1);

        scope.getSettings(HighlanderSettings.class).setHighlander((boolean) arguments.get(0));
    }

}
