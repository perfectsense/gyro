package beam.parser.ast;

import org.parboiled.trees.MutableTreeNodeImpl;

public class Node extends MutableTreeNodeImpl {

    public boolean addChild(Node node) {
        addChild(getChildren().size(), node);
        return true;
    }

}
