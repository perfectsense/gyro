package beam.lang;

public class BeamLiteral implements BeamResolvable {

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
    public boolean resolve(BeamConfig config) {
        return false;
    }

    @Override
    public Object getValue() {
        return literal;
    }

    public String toString() {
        return literal;
    }
}
