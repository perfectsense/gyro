package gyro.core.auth;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.DiffableScope;
import gyro.core.resource.Scope;

public class UseCredentialsDirectiveProcessor implements DirectiveProcessor {

    @Override
    public String getName() {
        return "use-credentials";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        if (diffableScope == null) {
            throw new GyroException("@use-credentials can only be used inside a resource!");
        }

        if (arguments.size() != 1) {
            throw new GyroException("@use-credentials directive only takes 1 argument!");
        }

        diffableScope.getSettings(CredentialsSettings.class)
            .setUseCredentials((String) arguments.get(0));
    }

}