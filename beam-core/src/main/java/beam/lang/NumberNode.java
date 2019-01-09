package beam.lang;

import org.apache.commons.lang.math.NumberUtils;

public class NumberNode extends ValueNode<Number> {

    private Number number;

    public NumberNode(String number) {
        this.number = NumberUtils.createNumber(number);
    }

    @Override
    public Number getValue() {
        return number;
    }

    @Override
    public NumberNode copy() {
        return new NumberNode(number.toString());
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
