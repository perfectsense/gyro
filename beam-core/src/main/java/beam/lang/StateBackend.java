package beam.lang;

public abstract class StateBackend {

    public abstract String name();

    public abstract BeamFile load(BeamFile fileNode) throws Exception;

    public abstract void save(BeamFile state);

    public abstract void delete(String path);

}
