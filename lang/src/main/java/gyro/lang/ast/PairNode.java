package gyro.lang.ast;

import com.google.common.base.Preconditions;
import gyro.parser.antlr4.GyroParser;

public class PairNode extends Node {

    private final Node key;
    private final Node value;

    public PairNode(Node key, Node value) {
        this.key = Preconditions.checkNotNull(key);
        this.value = Preconditions.checkNotNull(value);
    }

    public PairNode(GyroParser.PairContext context) {
        this(
            Node.create(Preconditions.checkNotNull(context).key().getChild(0)),
            Node.create(context.value()));
    }

    public Node getKey() {
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
