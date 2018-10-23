package beam.core;

public class BeamLiteral implements BeamReferable {

    private String literal;

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
}
