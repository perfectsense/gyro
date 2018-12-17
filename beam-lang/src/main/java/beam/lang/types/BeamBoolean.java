package beam.lang.types;

public class BeamBoolean extends BeamValue {

    private boolean value;

    public BeamBoolean(String value) {
        this.value = Boolean.valueOf(value);
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public String stringValue() {
        return Boolean.toString(value);
    }

}
