package beam.lang.types;

import beam.lang.Node;

import java.util.ArrayList;
import java.util.List;

public class StringExpressionValue extends Value<String> {

    private List<Value> values;

    public List<Value> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }

        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }

    @Override
    public String getValue() {
        StringBuilder sb = new StringBuilder();

        for (Value value : getValues()) {
            if (value.getValue() != null) {
                sb.append(value.getValue().toString());
            }
        }

        return sb.toString();
    }

    @Override
    public StringExpressionValue copy() {
        StringExpressionValue expressionNode = new StringExpressionValue();

        for (Value value : getValues()) {
            Value copy = value.copy();
            copy.parentNode(expressionNode);

            expressionNode.getValues().add(copy);
        }

        return expressionNode;
    }

    @Override
    public boolean resolve() {
        for (Node node : getValues()) {
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
