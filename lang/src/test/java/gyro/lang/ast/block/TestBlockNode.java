package gyro.lang.ast.block;

import java.util.List;

import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;

public class TestBlockNode extends BlockNode {

    public TestBlockNode(List<Node> body) {
        super(body);
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        throw new UnsupportedOperationException();
    }

}
