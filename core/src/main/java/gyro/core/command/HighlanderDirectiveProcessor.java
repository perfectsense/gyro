package gyro.core.command;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;

public class HighlanderDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "highlander";
    }

    @Override
    public void process(Scope scope, List<Object> arguments) throws Exception {
        if (!(scope instanceof RootScope)) {
            throw new GyroException("@highlander directive can only be used within the init.gyro file!");
        }

        if (!arguments.isEmpty()) {
            throw new GyroException("@highlander takes no arguments!");
        }

        scope.getSettings(HighlanderSettings.class).setHighlander(true);
    }

}
