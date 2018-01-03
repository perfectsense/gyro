package beam.parser.ast;

public class ASTKeyValue extends Node {

    private String key;

    private String value;

    public ASTKeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ASTKeyValue{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

