package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ResourceNode extends BlockNode {

    private final String type;
    private final Node nameNode;

    public ResourceNode(String type, Node nameNode, List<Node> body) {
        super(body);

        this.type = type;
        this.nameNode = nameNode;
    }

    public ResourceNode(GyroParser.ResourceContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        type = context.resourceType().getText();
        nameNode = Node.create(context.resourceName().getChild(0));
    }

    public String getType() {
        return type;
    }

    public Node getNameNode() {
        return nameNode;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitResource(this, context);
    }

}