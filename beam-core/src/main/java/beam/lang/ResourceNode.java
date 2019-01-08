package beam.lang;

import beam.core.BeamCore;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResourceNode extends ContainerNode {

    private String type;
    private String name;
    private StringExpressionNode nameExpression;

    private Set<ResourceNode> dependencies;
    private Set<ResourceNode> dependents;
    private Map<String, List<ResourceNode>> subResources;
    private BeamCore core;
    private RootNode rootNode;

    public Set<ResourceNode> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<ResourceNode> dependents() {
        if (dependents == null) {
            dependents = new LinkedHashSet<>();
        }

        return dependents;
    }

    public Map<String, List<ResourceNode>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public String resourceType() {
        return type;
    }

    public void setResourceType(String type) {
        this.type = type;
    }

    public String resourceIdentifier() {
        if (nameExpression != null) {
            return nameExpression.getValue();
        }

        return name;
    }

    public void setResourceIdentifier(String name) {
        this.name = name;
    }

    public void setResourceIdentifierExpression(StringExpressionNode nameExpression) {
        this.nameExpression = nameExpression;
    }

    public ResourceKey resourceKey() {
        return new ResourceKey(resourceType(), resourceIdentifier());
    }

    public RootNode rootNode() {
        if (rootNode == null) {
            Node parent = parentNode();

            while (parent != null && !(parent instanceof RootNode)) {
                parent = parent.parentNode();
            }

            if (parent instanceof RootNode) {
                rootNode = (RootNode) parent;
            }
        }

        return rootNode;
    }

    @Override
    public boolean resolve() {
        boolean resolved = super.resolve();

        if (nameExpression != null) {
            nameExpression.resolve();
        }

        for (List<ResourceNode> resources : subResources().values()) {
            for (ResourceNode resource : resources) {
                if (!resource.resolve()) {
                    throw new BeamLanguageException("Unable to resolve configuration.", resource);
                }
            }
        }

        if (resolved) {
            syncInternalToProperties();
        }

        return resolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(resourceType()).append(" ");

        if (resourceIdentifier() != null) {
            sb.append('\'');
            sb.append(resourceIdentifier());
            sb.append('\'');
        }

        sb.append("\n");
        sb.append(super.toString());

        sb.append("end\n\n");

        return sb.toString();
    }

    protected void syncInternalToProperties() {
    }

    /**
     * `execute()` is called during the parsing of the configuration. This
     * allows extensions to perform any necessary actions to load themselves.
     */
    public void execute() {

    }

    final void executeInternal() {
        syncInternalToProperties();
        execute();
    }

    public BeamCore core() {
        return core;
    }

    public void setCore(BeamCore core) {
        this.core = core;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceNode that = (ResourceNode) o;

        return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

}
