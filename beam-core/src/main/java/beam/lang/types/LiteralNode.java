package beam.lang.types;

public abstract class LiteralNode extends ValueNode<String> {

    private String literal;

    public LiteralNode(String literal) {
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
