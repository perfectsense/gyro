package beam.parser.ast;

import beam.core.diff.ResourceDiff;

import java.util.ArrayList;
import java.util.List;

/**
 * ASTBeamRoot represents a single parsed Beam configuration file.
 */
public class ASTBeamRoot extends Node {

    private List<Node> nodes;
    private ResourceDiff diff;

    public List<Node> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }

        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public ResourceDiff getDiff() {
        return diff;
    }

    public void setDiff(ResourceDiff diff) {
        this.diff = diff;
    }
}
