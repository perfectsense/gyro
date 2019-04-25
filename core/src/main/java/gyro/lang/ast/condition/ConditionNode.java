package gyro.lang.ast.condition;

import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public abstract class ConditionNode extends Node {

    private Node leftNode;
    private Node rightNode;

    public ConditionNode(GyroParser.ConditionContext context) {
        leftNode = Node.create(context.getChild(0));

        if (context.getChild(2) != null) {
            rightNode = Node.create(context.getChild(2));
        }
    }

    public Node getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    public Node getRightNode() {
        return rightNode;
    }

    public void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }

    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        return false;
    }

}
