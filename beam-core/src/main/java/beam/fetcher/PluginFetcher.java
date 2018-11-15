package beam.fetcher;

import beam.lang.BeamConfig;

public abstract class PluginFetcher {

    public abstract boolean validate(BeamConfig fetcherContext);

    public abstract void fetch(BeamConfig fetcherContext);
}
