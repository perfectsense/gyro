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
    public String toString() {
        return "KeyValueBlock{" +
            "key='" + key + '\'' +
            ", value=" + value +
            '}';
    }

}
