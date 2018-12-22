package beam.lang.nodes;

public abstract class ValueNode<T> extends Node {

    public abstract T getValue();

}
