package beam.lang.types;

import java.util.HashSet;
import java.util.Set;

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
    public boolean resolve() {
        return true;
    }

    @Override
    public String toString() {
        return Boolean.toString(getValue());
    }
}
