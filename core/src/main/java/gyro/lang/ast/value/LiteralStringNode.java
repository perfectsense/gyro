package gyro.lang.ast.value;

import gyro.lang.ast.Node;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;
import org.apache.commons.lang.StringUtils;

public class LiteralStringNode extends Node {

    private final String value;

    public LiteralStringNode(String value) {
        this.value = value;
    }

    public LiteralStringNode(GyroParser.LiteralStringContext context) {
        this(StringUtils.strip(context.STRING().getText(), "'"));
    }

    @Override
    public Object evaluate(Scope scope) {
        return value;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append('\"');
        builder.append(value);
        builder.append('\"');
    }
}
