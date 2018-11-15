package beam.core.extensions;

import beam.core.BeamException;
import beam.fetcher.PluginFetcher;
import beam.lang.BeamConfig;

import org.reflections.Reflections;
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

                Reflections reflections = new Reflections("beam.fetcher");
                boolean match = false;
                for (Class<? extends PluginFetcher> fetcherClass : reflections.getSubTypesOf(PluginFetcher.class)) {
                    try {
                        PluginFetcher fetcher = fetcherClass.newInstance();
                        if (fetcher.validate(path)) {
                            fetcher.fetch(path);
                            match = true;
                        }
                    } catch (IllegalAccessException | InstantiationException error) {
                        throw new BeamException(String.format("Unable to access %s", fetcherClass.getName()), error);
                    }
                }

                if (!match) {
                    throw new BeamException(String.format("Unable to fetch plugin %s", path));
                }
            }
        }
    }
}
