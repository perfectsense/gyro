package gyro.lang.ast.condition;

import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;

public class TestCompoundConditionNode extends CompoundConditionNode {

    public TestCompoundConditionNode(Node left, Node right) {
        super(left, right);
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        throw new UnsupportedOperationException();
    }

}
