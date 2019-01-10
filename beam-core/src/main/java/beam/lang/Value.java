package beam.lang;

public abstract class Value<T> extends Node {

    public abstract T getValue();

    public abstract Value copy();

}
