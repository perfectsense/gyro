package beam.lang.types;

import beam.lang.BeamLanguageException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeamReference extends BeamValue {

    private String type;
    private String name;
    private String attribute;
    private ContainerBlock referencedBlock;

    private Pattern partial = Pattern.compile("\\$\\((?<type>[^\\s]+) (?<name>[^\\s]+)\\)");
    private Pattern full = Pattern.compile("\\$\\((?<type>[^\\s]+) (?<name>[^\\s]+) \\| (?<attribute>[^\\s]+)\\)");

    public BeamReference(String fullRef) {
        Matcher partialMatcher = partial.matcher(fullRef);
        Matcher fullMatcher = full.matcher(fullRef);
        if (fullMatcher.find()) {
            this.type = fullMatcher.group("type");
            this.name = fullMatcher.group("name");
            this.attribute = fullMatcher.group("attribute");
        } else if (partialMatcher.find()) {
            this.type = partialMatcher.group("type");
            this.name = partialMatcher.group("name");
        }
    }

    public BeamReference(String type, String name) {
        this(type, name, null);
    }

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

    public ContainerBlock getReferencedBlock() {
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
        Node parent = getParentBlock();

        // Traverse up
        while (parent != null) {
            if (parent instanceof ContainerBlock) {
                ContainerBlock containerBlock = (ContainerBlock) parent;

                referencedBlock = containerBlock.getResource(getName(), getType());

                // Only ResourceBlocks have a dependency chain.
                if (referencedBlock != null  && referencedBlock instanceof ResourceBlock
                    && getParentBlock() != null && getParentBlock() instanceof ResourceBlock) {

                    ResourceBlock parentRef = (ResourceBlock) getParentBlock();
                    ResourceBlock resourceRef = (ResourceBlock) referencedBlock;
                    resourceRef.dependents().add(parentRef);
                    parentRef.dependencies().add(resourceRef);

                    return true;
                }
            }

            parent = parent.getParentBlock();
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
