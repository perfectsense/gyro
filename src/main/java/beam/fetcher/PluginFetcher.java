package beam.fetcher;

public abstract class PluginFetcher {

    public abstract boolean validate(String key);

    public abstract void fetch(String key);
}
