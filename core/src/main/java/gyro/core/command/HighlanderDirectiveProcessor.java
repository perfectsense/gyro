package gyro.core.command;

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
        validateArguments(node, 1, 1);
        scope.getSettings(HighlanderSettings.class).setHighlander(Boolean.TRUE.equals(getArgument(scope, node, Boolean.class, 0)));
    }

}
