package gyro.lang.ast.condition;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

public class ComparisonConditionNode extends Node {

    private final Node leftNode;
    private final String operator;
    private final Node rightNode;

    public ComparisonConditionNode(Node leftNode, String operator, Node rightNode) {
        this.leftNode = Preconditions.checkNotNull(leftNode);
        this.operator = Preconditions.checkNotNull(operator);
        this.rightNode = Preconditions.checkNotNull(rightNode);
    }

    public ComparisonConditionNode(GyroParser.ComparisonConditionContext context) {
        this(
            Node.create(Preconditions.checkNotNull(context).getChild(0)),
            context.comparisonOperator().getText(),
            Node.create(context.getChild(2)));
    }

    public Node getLeftNode() {
        return leftNode;
    }

    public String getOperator() {
        return operator;
    }

    public Node getRightNode() {
        return rightNode;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitComparisonCondition(this, context);
    }

}
