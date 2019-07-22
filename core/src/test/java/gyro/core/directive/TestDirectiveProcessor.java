package gyro.core.directive;

import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class TestDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
