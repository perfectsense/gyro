package gyro.core.workflow;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;

public class Swap {

    private final Node type;
    private final Node x;
    private final Node y;

    public Swap(Node type, Node x, Node y) {
        this.type = Preconditions.checkNotNull(type);
        this.x = Preconditions.checkNotNull(x);
        this.y = Preconditions.checkNotNull(y);
    }

    public Node getType() {
        return type;
    }

    public Node getX() {
        return x;
    }

    public Node getY() {
        return y;
    }

}
