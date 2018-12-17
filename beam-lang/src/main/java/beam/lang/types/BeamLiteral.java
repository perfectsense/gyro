package beam.lang.types;

public abstract class BeamLiteral extends BeamValue {

    private String literal;

    public BeamLiteral(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public String stringValue() {
        return literal;
    }

    @Override
    public String toString() {
        return literal;
    }
}
