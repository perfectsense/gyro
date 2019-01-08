package beam.lang;

import beam.core.diff.ResourceName;
import beam.parser.antlr4.BeamParser;

public class ReferenceNode extends ValueNode {

    private String type;
    private String name;
    private String attribute;
    private ContainerNode referencedBlock;
    private ValueNode valueNode;

    ReferenceNode(BeamParser.Reference_bodyContext context) {
        // $(reference_type reference_name | reference_attribute)
        if (context.reference_type() != null) {
            this.type = context.reference_type().getText();
        }

        if (context.reference_name() != null) {
            this.name = context.reference_name().getText();
        }

        if (context.reference_attribute() != null) {
            this.attribute = context.reference_attribute().getText();
        }

        // $(reference_name)
        if (context.reference_type() != null && context.reference_name() == null) {
            this.name = context.reference_type().getText();
            this.type = null;
        }
    }

    public ReferenceNode(String name) {
        this(null, name, null);
    }

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

    public ValueNode getValueNode() {
        return valueNode;
    }

    public boolean isSimpleValue() {
        return getName() != null && getType() == null;
    }

    @Override
    public Object getValue() {
        if (getReferencedBlock() != null && getAttribute() == null) {
            return getReferencedBlock();
        } else if (getReferencedBlock() != null && getAttribute() != null) {
            return getReferencedBlock().resolvedKeyValues().get(getAttribute());
        }

        if (getValueNode() != null) {
            return getValueNode().getValue();
        }

        return null;
    }

    public ResourceNode getParentResourceNode() {
        Node parent = parentNode();

        // Traverse up
        while (parent != null) {
            if (parent instanceof ResourceNode) {
                // Skip subresources
                ResourceName name = parent.getClass().getAnnotation(ResourceName.class);
                if (name != null && name.parent().equals("")) {
                    return (ResourceNode) parent;
                }
            }

            parent = parent.parentNode();
        }

        return null;
    }

    @Override
    public boolean resolve() {
        Node parent = getParentResourceNode();

        // Traverse up
        while (parent != null) {
            if (parent instanceof RootNode) {
                RootNode containerNode = (RootNode) parent;

                // Look for resources.
                referencedBlock = containerNode.getResource(getType(), getName());

                // Only ResourceBlocks have a dependency chain.
                if (referencedBlock != null  && getParentResourceNode() != null) {
                    ResourceNode parentRef = getParentResourceNode();
                    ResourceNode resourceRef = (ResourceNode) referencedBlock;

                    if (parentRef != resourceRef) {
                        resourceRef.dependents().add(parentRef);
                        parentRef.dependencies().add(resourceRef);
                    }

                    return true;
                }

                // Look for key/value pairs.
                if (isSimpleValue()) {
                    valueNode = containerNode.get(getName());
                    if (valueNode != null) {
                        return true;
                    }
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
            if (getType() != null) {
                sb.append(getType()).append(" ");
            }
            sb.append(getName());

            if (getAttribute() != null) {
                sb.append(" | ").append(getAttribute());
            }

            sb.append(")");
        }

        return sb.toString();
    }

}
