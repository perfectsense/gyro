package beam.core;

public abstract class BeamState<C extends BeamCredentials> {

    public abstract BeamResource loadState(BeamCredentials credentials, String name);

    public abstract void saveState(BeamCredentials credentials, String name, BeamResource resource);

    public abstract void deleteState(BeamCredentials credentials, String name);

}
