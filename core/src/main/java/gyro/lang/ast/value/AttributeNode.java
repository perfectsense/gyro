package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.QueryAttributeValueContext;

public class AttributeNode extends Node {

    private final String value;

    public AttributeNode(QueryAttributeValueContext context) {
        this.value = context.getText();
    }

    public AttributeNode(String value) {
        this.value = value;
    }

    @Override
    public Object evaluate(Scope scope) {
        return value;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(value);
    }

}
