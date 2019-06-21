package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class ValueNode extends Node {

    private final Object value;

    public ValueNode(Object value) {
        this.value = Preconditions.checkNotNull(value);
    }

    public ValueNode(GyroParser.BareStringContext context) {
        this(context.getText());
    }

    public ValueNode(GyroParser.BoolContext context) {
        this(context.TRUE() != null);
    }

    public ValueNode(GyroParser.LiteralStringContext context) {
        this(StringUtils.strip(context.getText(), "'"));
    }

    public ValueNode(GyroParser.NumberContext context) {
        this(NumberUtils.createNumber(context.getText()));
    }

    public Object getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValue(this, context);
    }

}
