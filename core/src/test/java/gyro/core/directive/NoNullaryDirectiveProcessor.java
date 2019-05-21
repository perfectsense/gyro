package gyro.core.directive;

import java.util.List;

import gyro.core.resource.Scope;

public class NoNullaryDirectiveProcessor extends DirectiveProcessor {

    private final Object object;

    public NoNullaryDirectiveProcessor(Object object) {
        this.object = object;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
    }

}
