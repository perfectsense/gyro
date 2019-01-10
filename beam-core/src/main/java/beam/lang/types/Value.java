package beam.lang.types;

import beam.lang.Node;

public abstract class Value<T> extends Node {

    public abstract T getValue();

    public abstract Value copy();

}
