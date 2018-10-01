package beam.fetcher;

public abstract class ProviderFetcher {

    public abstract boolean validate(String key);

    public abstract void fetch(String key);
}
