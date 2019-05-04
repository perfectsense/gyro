package gyro.lang.ast.condition;

import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public abstract class ConditionNode extends Node {

    private Node left;
    private Node right;

    public ConditionNode(GyroParser.ConditionContext context) {
        left = Node.create(context.getChild(0));

        if (context.getChild(2) != null) {
            right = Node.create(context.getChild(2));
        }
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
    }

}
