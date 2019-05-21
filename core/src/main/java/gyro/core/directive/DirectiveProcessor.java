package gyro.core.directive;

import java.util.List;

import gyro.core.resource.Scope;

public abstract class DirectiveProcessor {

    public abstract String getName();

    public abstract void process(Scope scope, List<Object> arguments) throws Exception;

}
