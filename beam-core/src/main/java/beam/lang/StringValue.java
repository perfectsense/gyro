package beam.lang;

import org.apache.commons.lang.StringUtils;

public class StringValue extends LiteralValue {

    public StringValue(String literal) {
        super(StringUtils.strip(literal, "'"));
    }

    @Override
    public boolean resolve() {
        return true;
    }

    @Override
    public String toString() {
        if (getLiteral() == null) {
            return null;
        }

        return "'" + getLiteral() + "'";
    }

    @Override
    public StringValue copy() {
        return new StringValue(getLiteral());
    }

}
