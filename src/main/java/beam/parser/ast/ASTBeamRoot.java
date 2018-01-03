package beam.parser.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * ASTBeamRoot represents a single parsed Beam configuration file.
 */
public class ASTBeamRoot extends Node {

    private List<Node> nodes;

    public List<Node> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }

        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
