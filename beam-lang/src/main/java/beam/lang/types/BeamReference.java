package beam.lang.types;

public class BeamReference extends BeamValue {

    private String type;
    private String name;
    private String attribute;
    private Object value;
    private BeamBlock referencedBlock;

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
    public Object getValue() {
        return null;
    }

    @Override
    public boolean resolve() {
        BeamBlock parent = getParentBlock();
        while (parent != null) {

            if (parent instanceof ContainerBlock) {
                ContainerBlock containerBlock = (ContainerBlock) parent;
                referencedBlock = containerBlock.get(getName(), getType());
                dependencies().add(getParentBlock());
                return true;
            }

            parent = parent.getParentBlock();
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("$(");
        sb.append(getType()).append(" ");
        sb.append(getName());

        if (getAttribute() != null) {
            sb.append("| ").append(getAttribute());
        }

        sb.append(")");

        return sb.toString();
    }

}
