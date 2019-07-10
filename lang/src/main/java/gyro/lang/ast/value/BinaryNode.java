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
        super(null);

        this.operator = Preconditions.checkNotNull(operator);
        this.left = Preconditions.checkNotNull(left);
        this.right = Preconditions.checkNotNull(right);
    }

    public BinaryNode(GyroParser.TwoAddContext context) {
        super(Preconditions.checkNotNull(context));

        this.operator = context.addOp().getText();
        this.left = Node.create(context.mul());
        this.right = Node.create(context.add());
    }

    public BinaryNode(GyroParser.TwoAndContext context) {
        super(Preconditions.checkNotNull(context));

        this.operator = context.AND().getText();
        this.left = Node.create(context.rel());
        this.right = Node.create(context.and());
    }

    public BinaryNode(GyroParser.TwoMulContext context) {
        super(Preconditions.checkNotNull(context));

        this.operator = context.mulOp().getText();
        this.left = Node.create(context.mulItem());
        this.right = Node.create(context.mul());
    }

    public BinaryNode(GyroParser.TwoRelContext context) {
        super(Preconditions.checkNotNull(context));

        this.operator = context.relOp().getText();
        this.left = Node.create(context.add());
        this.right = Node.create(context.rel());
    }

    public BinaryNode(GyroParser.TwoValueContext context) {
        super(Preconditions.checkNotNull(context));

        this.operator = context.OR().getText();
        this.left = Node.create(context.and());
        this.right = Node.create(context.value());
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
