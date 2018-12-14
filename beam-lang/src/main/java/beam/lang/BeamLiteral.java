package beam.lang;

import java.util.HashSet;
import java.util.Set;

public class BeamLiteral implements BeamReferable {

    private String literal;

    public BeamLiteral() {
    }

    public BeamLiteral(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public boolean resolve(BeamContext context) {
        return false;
    }

    @Override
    public Object getValue() {
        return literal;
    }

    public String toString() {
        return "'" + literal + "'";
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        return new HashSet<>();
    }
}
