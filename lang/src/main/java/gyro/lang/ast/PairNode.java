package gyro.lang.ast;

import com.google.common.base.Preconditions;
import gyro.parser.antlr4.GyroParser;

public class PairNode extends Node {

    private final String key;
    private final Node valueNode;

    public PairNode(String key, Node valueNode) {
        this.key = Preconditions.checkNotNull(key);
        this.valueNode = Preconditions.checkNotNull(valueNode);
    }

    public PairNode(GyroParser.PairContext context) {
        this(
            Preconditions.checkNotNull(context).key().getChild(0).getText(),
            Node.create(context.value().getChild(0)));
    }

    public String getKey() {
        return key;
    }

    public Node getValueNode() {
        return valueNode;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitPair(this, context);
    }

}
