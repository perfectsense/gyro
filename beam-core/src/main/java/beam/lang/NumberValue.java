package beam.lang;

import org.apache.commons.lang.math.NumberUtils;

public class NumberValue extends Value<Number> {

    private Number number;

    public NumberValue(String number) {
        this.number = NumberUtils.createNumber(number);
    }

    @Override
    public Number getValue() {
        return number;
    }

    @Override
    public NumberValue copy() {
        return new NumberValue(number.toString());
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
