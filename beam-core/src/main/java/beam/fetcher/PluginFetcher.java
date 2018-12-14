package beam.fetcher;

import beam.lang.BeamBlockMethod;

public interface PluginFetcher {

    boolean validate(BeamBlockMethod fetcherContext);

    void fetch(BeamBlockMethod fetcherContext);
}
