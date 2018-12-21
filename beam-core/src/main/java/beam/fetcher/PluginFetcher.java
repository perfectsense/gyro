package beam.fetcher;

import beam.lang.BeamLanguageExtension;

public interface PluginFetcher {

    boolean validate(BeamLanguageExtension fetcherContext);

    void fetch(BeamLanguageExtension fetcherContext);

}
