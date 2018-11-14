package beam.core.extensions;

import beam.lang.BeamConfig;

import java.util.List;

public class ResourceExtension extends MethodExtension {

    @Override
    public String getName() {
        return "resource";
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {

    }

}
