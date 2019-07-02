package gyro.core.directive;

import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class NoNullaryDirectiveProcessor extends DirectiveProcessor {

    private final Object object;

    public NoNullaryDirectiveProcessor(Object object) {
        this.object = object;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
