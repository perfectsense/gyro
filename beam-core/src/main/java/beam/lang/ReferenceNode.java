package beam.lang;

public class ReferenceNode extends ValueNode {

    private String type;
    private String name;
    private String attribute;
    private ContainerNode referencedBlock;

    public ReferenceNode(String type, String name) {
        this(type, name, null);
    }

    public ReferenceNode(String type, String name, String attribute) {
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

    public ContainerNode getReferencedBlock() {
        return referencedBlock;
    }

    @Override
    public Object getValue() {
        if (getReferencedBlock() != null && getAttribute() == null) {
            return getReferencedBlock();
        } else if (getReferencedBlock() != null && getAttribute() != null) {
            return getReferencedBlock().resolvedKeyValues().get(getAttribute());
        }

        return null;
    }

    @Override
    public boolean resolve() {
        Node parent = getParentNode();

        // Traverse up
        while (parent != null) {
            if (parent instanceof RootNode) {
                RootNode containerNode = (RootNode) parent;

                referencedBlock = containerNode.getResource(getName(), getType());

                // Only ResourceBlocks have a dependency chain.
                if (referencedBlock != null  && referencedBlock instanceof ResourceNode
                    && getParentNode() != null && getParentNode() instanceof ResourceNode) {

                    ResourceNode parentRef = (ResourceNode) getParentNode();
                    ResourceNode resourceRef = (ResourceNode) referencedBlock;
                    resourceRef.dependents().add(parentRef);
                    parentRef.dependencies().add(resourceRef);

                    return true;
                }
            }

            parent = parent.getParentNode();
        }

        throw new BeamLanguageException("Unable to resolve reference.", this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Object value = getValue();
        if (value != null && value instanceof String) {
            sb.append("'" + value + "'");
        } else if (value != null && !(value instanceof Node)) {
            sb.append(value);
        } else {
            sb.append("$(");
            sb.append(getType()).append(" ");
            sb.append(getName());

            if (getAttribute() != null) {
                sb.append(" | ").append(getAttribute());
            }

            sb.append(")");
        }

        return sb.toString();
    }

}
