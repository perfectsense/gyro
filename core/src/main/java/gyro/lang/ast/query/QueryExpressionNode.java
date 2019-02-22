package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public abstract class QueryExpressionNode extends Node {

    private Resource resource;
    private Node leftNode;
    private Node rightNode;

    public QueryExpressionNode(BeamParser.QueryExpressionContext context) {
        leftNode = Node.create(context.getChild(0));

        if (context.getChild(2) != null) {
            rightNode = Node.create(context.getChild(2));
        }
    }

    public abstract Object evaluate(Resource resource, List<Resource> resources) throws Exception;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Node getLeftNode() {
        return leftNode;
    }

    public QueryExpressionNode getLeftQueryExpressionNode() {
        if (leftNode instanceof QueryExpressionNode) {
            return (QueryExpressionNode) leftNode;
        }

        return null;
    }

    public void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    public Node getRightNode() {
        return rightNode;
    }

    public QueryExpressionNode getRightQueryExpressionNode() {
        if (rightNode instanceof QueryExpressionNode) {
            return (QueryExpressionNode) rightNode;
        }

        return null;
    }

    public void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }

}
