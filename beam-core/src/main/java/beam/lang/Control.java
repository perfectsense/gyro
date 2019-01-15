package beam.lang;

public abstract class Control extends Node {

    public abstract void evaluate();

    @Override
    public String serialize(int indent) {
        return "";
    }

}
