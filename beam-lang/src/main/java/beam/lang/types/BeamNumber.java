package beam.lang.types;

import org.apache.commons.lang.math.NumberUtils;

public class BeamNumber extends BeamValue<Number> {

    private Number number;

    public BeamNumber(String number) {
        this.number = NumberUtils.createNumber(number);
    }

    @Override
    public Number getValue() {
        return number;
    }

    @Override
    public boolean resolve() {
        return true;
    }

    @Override
    public String toString() {
        return number.toString();
    }

}
