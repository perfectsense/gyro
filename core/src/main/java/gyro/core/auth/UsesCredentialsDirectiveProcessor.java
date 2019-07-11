package gyro.core.auth;

import java.util.List;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

public class UsesCredentialsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public String getName() {
        return "uses-credentials";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 1);

        scope.getSettings(CredentialsSettings.class).setUseCredentials((String) arguments.get(0));
    }

}