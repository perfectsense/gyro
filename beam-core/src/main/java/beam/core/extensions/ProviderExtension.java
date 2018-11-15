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
        methodContext.resolve(globalContext);
        Reflections reflections = new Reflections("beam.fetcher");
        boolean match = false;
        for (Class<? extends PluginFetcher> fetcherClass : reflections.getSubTypesOf(PluginFetcher.class)) {
            try {
                PluginFetcher fetcher = fetcherClass.newInstance();
                if (fetcher.validate(methodContext)) {
                    fetcher.fetch(methodContext);
                    match = true;
                }
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to access %s", fetcherClass.getName()), error);
            }
        }

        if (!match) {
            throw new BeamException(String.format("Unable to find fetcher matching:\n %s", methodContext));
        }
    }
}
