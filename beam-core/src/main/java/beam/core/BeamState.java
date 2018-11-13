package beam.core;

public abstract class BeamState<C extends BeamCloud> {

    public abstract BeamResource<C > loadState(BeamCloud cloud, String name);

    public abstract void saveState(BeamCloud cloud, String name, BeamResource<C> resource);

    public abstract void deleteState(BeamCloud cloud, String name);

}
