package gyro.core.auth;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;

public class UsesCredentialsDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "uses-credentials";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        if (diffableScope == null) {
            throw new GyroException("@uses-credentials can only be used inside a resource!");
        }

        List<Object> arguments = resolveArguments(scope, node);

        if (arguments.size() != 1) {
            throw new GyroException("@uses-credentials directive only takes 1 argument!");
        }

        diffableScope.getSettings(CredentialsSettings.class)
            .setUseCredentials((String) arguments.get(0));
    }

}