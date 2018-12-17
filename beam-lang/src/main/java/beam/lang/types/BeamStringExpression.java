package beam.lang.types;

import org.apache.commons.lang.StringUtils;

import java.util.Set;

public class BeamStringExpression extends BeamLiteral implements BeamReferable {

    public BeamStringExpression(String literal) {
        super(StringUtils.strip(literal, "\""));
    }

    @Override
    public boolean resolve(ContainerBlock context) {
        return false;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        return null;
    }

    @Override
    public String toString() {
        return "\"" + getLiteral() + "\"";
    }

}
