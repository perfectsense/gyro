package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ValueNode extends Node {

    private final Object value;

    public ValueNode(Object value) {
        this.value = Preconditions.checkNotNull(value);
    }

    public ValueNode(GyroParser.ValueContext context) {
        this(Node.create(Preconditions.checkNotNull(context).getChild(0)));
    }

    public Object getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitValue(this, context);
    }

}
