package beam.lang;

import java.util.ArrayList;
import java.util.List;

public class StringExpressionNode extends ValueNode<String> {

    private List<ValueNode> valueNodes;

    public List<ValueNode> getValueNodes() {
        if (valueNodes == null) {
            valueNodes = new ArrayList<>();
        }

        return valueNodes;
    }

    public void setValueNodes(List<ValueNode> valueNodes) {
        this.valueNodes = valueNodes;
    }

    @Override
    public String getValue() {
        StringBuilder sb = new StringBuilder();

        for (ValueNode valueNode : getValueNodes()) {
            if (valueNode.getValue() != null) {
                sb.append(valueNode.getValue().toString());
            }
        }

        return sb.toString();
    }

    @Override
    public boolean resolve() {
        for (Node node : getValueNodes()) {
            if (!node.resolve()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "\"" + getValue() + "\"";
    }

}
