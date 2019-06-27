package gyro.core.workflow;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;

public class Create {

    private final Node type;
    private final Node name;
    private final List<Node> body;

    public Create(Node type, Node name, List<Node> body) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public Node getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

    public List<Node> getBody() {
        return body;
    }

}
