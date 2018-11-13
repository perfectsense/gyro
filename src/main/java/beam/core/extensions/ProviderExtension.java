package beam.core.extensions;

import beam.core.BeamException;
import beam.fetcher.LocalFetcher;
import beam.lang.BeamConfig;

import java.util.List;

public class ProviderExtension extends MethodExtension {

    @Override
    public String getName() {
        return "provider";
    }

    @Override
    public void call(BeamConfig globalContext, List<String> arguments, BeamConfig methodContext) {
        if (methodContext.get("path") != null) {
            if (methodContext.get("path").resolve(globalContext)) {
                String path = (String) methodContext.get("path").getValue();

                LocalFetcher local = new LocalFetcher();
                if (local.validate(path)) {
                    local.fetch(path);

                    // Load all BeamResource classes
                } else {
                    throw new BeamException(String.format("Unable to find provider for path: %s", path));
                }
            }
        }
    }

}
