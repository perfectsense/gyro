package gyro.core.directive;

import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class PrivateDirectiveProcessor extends DirectiveProcessor<Scope> {

    private PrivateDirectiveProcessor() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
