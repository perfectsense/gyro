package gyro.core.directive;

import java.util.List;

import gyro.core.resource.Scope;

public interface DirectiveProcessor {

    String getName();

    void process(Scope scope, List<Object> arguments) throws Exception;

}
