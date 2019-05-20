package gyro.lang.ast.condition;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;

public abstract class CompoundConditionNode extends Node {

    private final Node left;
    private final Node right;

    public CompoundConditionNode(Node left, Node right) {
        this.left = Preconditions.checkNotNull(left);
        this.right = Preconditions.checkNotNull(right);
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

}
