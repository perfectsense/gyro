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
        super(null);

        this.value = Preconditions.checkNotNull(value);
    }

    public ValueNode(GyroParser.BoolContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = context.TRUE() != null;
    }

    public ValueNode(GyroParser.LiteralStringContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = StringUtils.strip(context.getText(), "'");
    }

    public ValueNode(GyroParser.NumberContext context) {
        super(Preconditions.checkNotNull(context));

        this.value = NumberUtils.createNumber(context.getText());
    }

    public Object getValue() {
        return value;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitValue(this, context);
    }

}
