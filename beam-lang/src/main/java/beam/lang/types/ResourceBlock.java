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
        StringBuilder sb = new StringBuilder();

        sb.append(getType()).append(" ");
        sb.append(getName()).append("\n");

        for (BeamBlock block : getBlocks()) {
            for (String line : block.toString().split("\n")) {
                sb.append("    " + line + "\n");
            }
        }

        sb.append("end\n\n");

        return sb.toString();
    }
}
