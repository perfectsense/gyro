package beam.lang.types;

public class BeamBoolean extends BeamValue<Boolean> {

    private boolean value;

    public BeamBoolean(String value) {
        this.value = Boolean.valueOf(value);
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(getValue());
    }
}
