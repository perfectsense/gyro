package gyro.core.directive;

import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class TestDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
