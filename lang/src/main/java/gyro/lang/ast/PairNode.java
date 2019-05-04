package gyro.lang.ast;

import com.google.common.base.Preconditions;
import gyro.parser.antlr4.GyroParser;

public class PairNode extends Node {

    private final String key;
    private final Node value;

    public PairNode(String key, Node value) {
        this.key = Preconditions.checkNotNull(key);
        this.value = Preconditions.checkNotNull(value);
    }

    public PairNode(GyroParser.PairContext context) {
        this(
            Preconditions.checkNotNull(context).key().getChild(0).getText(),
            Node.create(context.value().getChild(0)));
    }

    public String getKey() {
        return key;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitPair(this, context);
    }

}
