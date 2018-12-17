package beam.lang.types;

import java.util.Set;

public class BeamReference extends BeamValue implements BeamReferable {

    private String type;
    private String name;
    private String attribute;

    public BeamReference(String type, String name, String attribute) {
        this.type = type;
        this.name = name;
        this.attribute = attribute;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getAttribute() {
        return attribute;
    }

    @Override
    public boolean resolve(ContainerBlock context) {
        return false;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        return null;
    }

    @Override
    public String stringValue() {
        return null;
    }

    @Override
    public String toString() {
        return "BeamReference{" +
            "type='" + type + '\'' +
            ", name='" + name + '\'' +
            ", attribute='" + attribute + '\'' +
            '}';
    }

}
