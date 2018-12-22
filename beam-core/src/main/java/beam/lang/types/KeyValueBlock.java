package beam.lang.types;

public class KeyValueBlock extends Node {

    private String key;
    private ValueNode value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ValueNode getValue() {
        return value;
    }

    public void setValue(ValueNode value) {
        this.value = value;
    }


    @Override
    public void setParentBlock(Node parentBlock) {
        super.setParentBlock(parentBlock);

        if (getValue() != null) {
            getValue().setParentBlock(parentBlock);
        }
    }

    @Override
    public boolean resolve() {
        return getValue().resolve();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getKey()).append(": ");
        sb.append(getValue().toString());

        return sb.toString();
    }

}
