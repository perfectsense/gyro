package beam.lang.types;

import org.apache.commons.lang.math.NumberUtils;

public class BeamNumber extends BeamValue {

    private Number number;

    public BeamNumber(String number) {
        this.number = NumberUtils.createNumber(number);
    }

    @Override
    public String stringValue() {
        return toString();
    }

    @Override
    public String toString() {
        return number.toString();
    }

}
