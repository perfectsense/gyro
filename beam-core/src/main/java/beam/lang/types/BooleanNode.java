package beam.lang.types;

public class BooleanNode extends ValueNode<Boolean> {

    private boolean value;

    public BooleanNode(String value) {
        this.value = Boolean.valueOf(value);
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public boolean resolve() {
        return true;
    }

    @Override
    public String toString() {
        return Boolean.toString(getValue());
    }
}
