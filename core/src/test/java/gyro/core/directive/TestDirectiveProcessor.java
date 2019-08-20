package gyro.core.directive;

import gyro.core.Type;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("test")
public class TestDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
    }

}
