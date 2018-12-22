package beam.lang.nodes;

import org.apache.commons.lang.StringUtils;

public class StringNode extends LiteralNode {

    public StringNode(String literal) {
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

}
