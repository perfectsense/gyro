package gyro.lang.ast.value;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class BinaryNode extends Node {

    private final String operator;
    private final Node left;
    private final Node right;

    public BinaryNode(String operator, Node left, Node right) {
        this.operator = Preconditions.checkNotNull(operator);
        this.left = Preconditions.checkNotNull(left);
        this.right = Preconditions.checkNotNull(right);
    }

    public BinaryNode(GyroParser.TwoAddContext context) {
        this(
            Preconditions.checkNotNull(context).addOp().getText(),
            Node.create(context.mul()),
            Node.create(context.add()));
    }

    public BinaryNode(GyroParser.TwoAndContext context) {
        this(
            Preconditions.checkNotNull(context).AND().getText(),
            Node.create(context.rel()),
            Node.create(context.and()));
    }

    public BinaryNode(GyroParser.TwoMulContext context) {
        this(
            Preconditions.checkNotNull(context).mulOp().getText(),
            Node.create(context.mulItem()),
            Node.create(context.mul()));
    }

    public BinaryNode(GyroParser.TwoRelContext context) {
        this(
            Preconditions.checkNotNull(context).relOp().getText(),
            Node.create(context.add()),
            Node.create(context.rel()));
    }

    public BinaryNode(GyroParser.TwoValueContext context) {
        this(
            Preconditions.checkNotNull(context).OR().getText(),
            Node.create(context.and()),
            Node.create(context.value()));
    }

    public String getOperator() {
        return operator;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitBinary(this, context);
    }

}
