package gyro.core.auth;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

public class UsesCredentialsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public String getName() {
        return "uses-credentials";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        scope.getSettings(CredentialsSettings.class).setUseCredentials(getArgument(scope, node, String.class, 0));
        scope.getStateNodes().add(node);
    }

}