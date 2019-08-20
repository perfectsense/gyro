package gyro.core.directive;

import gyro.core.Type;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("no-nullary")
public class NoNullaryDirectiveProcessor extends DirectiveProcessor<Scope> {

    private final Object object;

    public NoNullaryDirectiveProcessor(Object object) {
        this.object = object;
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
