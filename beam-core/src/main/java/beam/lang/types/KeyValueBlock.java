package beam.lang.types;

public class KeyValueBlock extends Node {

    private String key;
    private BeamValue value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public BeamValue getValue() {
        return value;
    }

    public void setValue(BeamValue value) {
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
