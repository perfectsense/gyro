package beam.lang.types;

public abstract class BeamValue<T> extends BeamReferable {

    public abstract T getValue();

}
