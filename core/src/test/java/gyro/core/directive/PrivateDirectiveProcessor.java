package gyro.core.directive;

import java.util.List;

import gyro.core.resource.Scope;

public class PrivateDirectiveProcessor extends DirectiveProcessor {

    private PrivateDirectiveProcessor() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
    }

}
