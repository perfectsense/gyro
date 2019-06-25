package gyro.lang.ast.block;

import com.google.common.base.Preconditions;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class FileNode extends BlockNode {

    public FileNode(GyroParser.FileContext context) {
        super(Node.create(Preconditions.checkNotNull(context).statement()));
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitFile(this, context);
    }

}
