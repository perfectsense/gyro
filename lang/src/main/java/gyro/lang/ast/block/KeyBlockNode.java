package gyro.lang.ast.block;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class KeyBlockNode extends BlockNode {

    private final String key;
    private final Node name;

    public KeyBlockNode(String key, Node name, List<Node> body) {
        super(body);

        this.key = Preconditions.checkNotNull(key);
        this.name = name;
    }

    public KeyBlockNode(GyroParser.KeyBlockContext context) {
        this(
            Preconditions.checkNotNull(context).IDENTIFIER().getText(),
            Optional.ofNullable(context.name()).map(Node::create).orElse(null),
            Node.create(context.blockBody().blockStatement()));
    }

    public String getKey() {
        return key;
    }

    public Node getName() {
        return name;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitKeyBlock(this, context);
    }

}
