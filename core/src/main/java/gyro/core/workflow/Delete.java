package gyro.core.workflow;

import com.google.common.base.Preconditions;
import gyro.lang.ast.Node;

public class Delete {

    private final Node type;
    private final Node name;

    public Delete(Node type, Node name) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
    }

    public Node getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

}
