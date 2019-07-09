package gyro.lang.ast.block;

import java.util.List;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ResourceNode extends BlockNode {

    private final String type;
    private final Node name;

    public ResourceNode(String type, Node name, List<Node> body) {
        super(body);

        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
    }

    public ResourceNode(GyroParser.ResourceContext context) {
        this(
            Preconditions.checkNotNull(context).type().getText(),
            Node.create(context.name()),
            Node.create(context.body()));
    }

    public String getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitResource(this, context);
    }

}
