package beam.lang.types;

public abstract class BeamLiteral extends BeamValue<String> {

    private String literal;

    public BeamLiteral(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return literal;
    }

    @Override
    public String getValue() {
        return literal;
    }

}
