package beam.lang;

import org.apache.commons.lang.StringUtils;

public class StringExpressionNode extends LiteralNode {

    public StringExpressionNode(String literal) {
        super(StringUtils.strip(literal, "\""));
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

        return "\"" + getLiteral() + "\"";
    }

}
