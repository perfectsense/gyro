package beam.lang.types;

public abstract class LiteralValue extends Value<String> {

    private String literal;

    public LiteralValue(String literal) {
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
