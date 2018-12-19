package beam.lang.types;

import org.apache.commons.lang.StringUtils;

public class BeamString extends BeamLiteral {

    public BeamString(String literal) {
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
