package beam.lang.ast.block;

import beam.lang.ast.Node;

import java.util.List;

public abstract class BlockNode extends Node {

    protected final List<Node> body;

    public BlockNode(List<Node> body) {
        this.body = body;
    }

}
