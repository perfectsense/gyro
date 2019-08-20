package gyro.core.directive;

import gyro.core.Type;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("private")
public class PrivateDirectiveProcessor extends DirectiveProcessor<Scope> {

    private PrivateDirectiveProcessor() {
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
