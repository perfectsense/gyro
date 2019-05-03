package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class KeyBlockNode extends BlockNode {

    private final String key;

    public KeyBlockNode(String key, List<Node> body) {
        super(body);

        this.key = key;
    }

    public KeyBlockNode(GyroParser.ResourceContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

        key = context.resourceType().getText();
    }

    public String getKey() {
        return key;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitKeyBlock(this, context);
    }

}