package beam.providerBuilder;

public abstract class ProviderBuilder {

    public abstract boolean validate(String path);

    public abstract String build(String path);
}
