package beam.lang.ast.block;

import java.util.List;

import beam.lang.ast.Node;

public abstract class BlockNode extends Node {

    protected final List<Node> body;

    public BlockNode(List<Node> body) {
        this.body = body;
    }

}
