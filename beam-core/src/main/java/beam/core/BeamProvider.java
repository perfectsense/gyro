package beam.core;

import beam.fetcher.PluginFetcher;
import beam.lang.BeamLanguageExtension;
import org.reflections.Reflections;

public class BeamProvider extends BeamLanguageExtension {

    @Override
    public String getType() {
        return "provider";
    }

    @Override
    public void execute() {
        Reflections reflections = new Reflections("beam.fetcher");
        boolean match = false;
        for (Class<? extends PluginFetcher> fetcherClass : reflections.getSubTypesOf(PluginFetcher.class)) {
            try {
                PluginFetcher fetcher = fetcherClass.newInstance();
                if (fetcher.validate(this)) {
                    fetcher.fetch(this);
                    match = true;
                }
            } catch (IllegalAccessException | InstantiationException error) {
                throw new BeamException(String.format("Unable to access %s", fetcherClass.getName()), error);
            }
        }

        if (!match) {
            throw new BeamException(String.format("Unable to find fetcher matching:\n %s", this));
        }
    }

}
