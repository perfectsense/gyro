package gyro.lang.ast.block;

import java.util.List;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class KeyBlockNode extends BlockNode {

    private final String key;

    public KeyBlockNode(String key, List<Node> body) {
        super(body);

        this.key = Preconditions.checkNotNull(key);
    }

    public KeyBlockNode(GyroParser.KeyBlockContext context) {
        this(
            Preconditions.checkNotNull(context).IDENTIFIER().getText(),
            Node.create(context.blockBody().blockStatement()));
    }

    public String getKey() {
        return key;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitKeyBlock(this, context);
    }

}
