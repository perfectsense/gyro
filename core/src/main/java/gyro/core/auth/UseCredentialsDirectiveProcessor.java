package gyro.core.auth;

import java.util.List;

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
        scope.getClosest(DiffableScope.class)
            .getSettings(CredentialsSettings.class)
            .setUseCredentials((String) arguments.get(0));
    }

}