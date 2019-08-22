package gyro.core.auth;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("uses-credentials")
public class UsesCredentialsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        scope.getSettings(CredentialsSettings.class).setUseCredentials(getArgument(scope, node, String.class, 0));
        scope.getStateNodes().add(node);
    }

}