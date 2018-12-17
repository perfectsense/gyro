package beam.lang.types;

import org.apache.commons.lang.math.NumberUtils;

public class BeamNumber extends BeamValue<Number> {

    private Number number;

    public BeamNumber(String number) {
        this.number = NumberUtils.createNumber(number);
    }

    @Override
    public String toString() {
        return number.toString();
    }

    @Override
    public Number getValue() {
        return number;
    }

}
