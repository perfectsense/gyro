package beam.fetcher;

import beam.lang.BeamExtension;

public interface PluginFetcher {

    boolean validate(BeamExtension fetcherContext);

    void fetch(BeamExtension fetcherContext);
}
