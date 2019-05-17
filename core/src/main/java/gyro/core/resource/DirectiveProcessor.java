package gyro.core.resource;

import java.util.List;

public interface DirectiveProcessor {

    String getName();

    void process(Scope scope, List<Object> arguments) throws Exception;

}
