package gyro.lang.ast.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;

import java.util.List;

public abstract class BlockNode extends Node {

    private final List<Node> body;

    public BlockNode(List<Node> body) {
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public List<Node> getBody() {
        return body;
    }

}
