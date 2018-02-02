package beam.providerHandler;

public abstract class ProviderHandler {

    public abstract boolean validate(String key);

    public abstract void handle(String key);
}
