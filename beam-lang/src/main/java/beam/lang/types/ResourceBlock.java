package beam.lang.types;

public class ResourceBlock extends ContainerBlock {

    private String type;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ResourceBlock{" +
            "type='" + type + '\'' +
            ", name='" + name + '\'' +
            ", blocks=" + getBlocks() +
            '}';
    }
}
