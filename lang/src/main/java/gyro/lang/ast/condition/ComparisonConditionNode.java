package gyro.lang.ast.condition;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class ComparisonConditionNode extends Node {

    private final Node left;
    private final String operator;
    private final Node right;

    public ComparisonConditionNode(Node left, String operator, Node right) {
        this.left = Preconditions.checkNotNull(left);
        this.operator = Preconditions.checkNotNull(operator);
        this.right = Preconditions.checkNotNull(right);
    }

    public ComparisonConditionNode(GyroParser.ComparisonConditionContext context) {
        this(
            Node.create(Preconditions.checkNotNull(context).getChild(0)),
            context.comparisonOperator().getText(),
            Node.create(context.getChild(2)));
    }

    public Node getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public Node getRight() {
        return right;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitComparisonCondition(this, context);
    }

}
