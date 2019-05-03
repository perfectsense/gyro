package gyro.lang.ast.block;

import java.util.stream.Collectors;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class FileNode extends BlockNode {

    public FileNode(GyroParser.FileContext context) {
        super(context.statement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitFile(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildBody(builder, indentDepth, body);
    }

}
