package beam.lang;

public abstract class ValueNode<T> extends Node {

    public abstract T getValue();

}
