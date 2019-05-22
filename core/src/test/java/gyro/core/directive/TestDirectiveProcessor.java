package gyro.core.directive;

import java.util.List;

import gyro.core.resource.Scope;

public class TestDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) {
    }

}
