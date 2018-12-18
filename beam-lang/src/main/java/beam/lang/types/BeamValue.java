package beam.lang.types;

public abstract class BeamValue<T> extends BeamReferable {

    public abstract T getValue();

    public boolean resolve(ResourceBlock parent) {
        return true;
    }

}
