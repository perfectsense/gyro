package beam.lang.types;

import org.apache.commons.lang.StringUtils;

public class BeamStringExpression extends BeamLiteral {

    public BeamStringExpression(String literal) {
        super(StringUtils.strip(literal, "\""));
    }

    @Override
    public boolean resolve() {
        return true;
    }

    @Override
    public String toString() {
        return "\"" + getLiteral() + "\"";
    }

}
