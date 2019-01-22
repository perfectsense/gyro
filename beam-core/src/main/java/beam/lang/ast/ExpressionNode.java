package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

public abstract class ExpressionNode extends Node {

    private Node leftNode;
    private Node rightNode;

    public ExpressionNode(BeamParser.ExpressionContext context) {
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
}
