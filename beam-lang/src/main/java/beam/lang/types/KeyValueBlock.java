package beam.lang.types;

public class KeyValueBlock extends BeamBlock {

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
    public boolean resolve() {
        if (getParentBlock() instanceof ResourceBlock) {
            return getValue().resolve((ResourceBlock) getParentBlock());
        }

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
