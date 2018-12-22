package beam.lang.types;

public abstract class BeamValue<T> extends Node {

    public abstract T getValue();

}
