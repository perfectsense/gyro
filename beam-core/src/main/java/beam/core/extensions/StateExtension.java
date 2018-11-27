package beam.core.extensions;

import beam.core.BeamException;
import beam.core.BeamLocalState;
import beam.core.BeamState;
import beam.lang.BeamConfig;
import beam.lang.BeamConfigKey;

import java.util.List;

public class StateExtension extends MethodExtension {

    @Override
    public String getName() {
        return "state";
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {
        if (arguments.size() == 1) {
            if ("local".equals(arguments.get(0))) {
                BeamState state = new BeamLocalState();
                BeamConfigKey key = new BeamConfigKey(state.getClass().getSimpleName(), arguments.get(0));
                globalContext.getContext().put(key, state);

                return;
            }
        }

        throw new BeamException("Unable to load state extension.");
    }

}
