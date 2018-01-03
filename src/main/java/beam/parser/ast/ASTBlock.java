package beam.parser.ast;

import java.util.List;

public class ASTBlock extends Node {

    @Override
    public String toString() {
        return "ASTBlock{" +
                "nodes=" + nodes +
                '}';
    }

    private List<Node> nodes;

}
