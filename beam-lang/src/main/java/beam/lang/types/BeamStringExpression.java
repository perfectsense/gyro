package beam.lang.types;

import org.apache.commons.lang.StringUtils;

import java.util.Set;

public class BeamStringExpression extends BeamLiteral {

    public BeamStringExpression(String literal) {
        super(StringUtils.strip(literal, "\""));
    }

    @Override
    public boolean resolve() {
        return false;
    }

    @Override
    public String toString() {
        return "\"" + getLiteral() + "\"";
    }

}
