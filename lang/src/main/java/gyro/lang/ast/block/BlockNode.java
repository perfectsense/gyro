package gyro.lang.ast.block;

import gyro.lang.ast.Node;

import java.util.List;

public abstract class BlockNode extends Node {

    protected final List<Node> body;

    public BlockNode(List<Node> body) {
        this.body = body;
    }

    public List<Node> getBody() {
        return body;
    }

}
