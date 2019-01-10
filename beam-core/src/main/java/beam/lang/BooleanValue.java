package beam.lang;

public class BooleanValue extends Value<Boolean> {

    private boolean value;

    public BooleanValue(String value) {
        this.value = Boolean.valueOf(value);
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public BooleanValue copy() {
        return new BooleanValue(Boolean.toString(value));
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
