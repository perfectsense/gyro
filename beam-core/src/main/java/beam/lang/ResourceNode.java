package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamException;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

public class ResourceNode extends ContainerNode {

    private String type;
    private String name;

    private Set<ResourceNode> dependencies;
    private Set<ResourceNode> dependents;
    private BeamCore core;

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

    public String getResourceType() {
        return type;
    }

    public void setResourceType(String type) {
        this.type = type;
    }

    public String getResourceIdentifier() {
        return name;
    }

    public void setResourceIdentifier(String name) {
        this.name = name;
    }

    public ResourceKey resourceKey() {
        return new ResourceKey(getResourceType(), getResourceIdentifier());
    }

    @Override
    public boolean resolve() {
        boolean resolved = super.resolve();

        if (resolved) {
            syncInternalToProperties();
        }

        return resolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getResourceType()).append(" ");
        sb.append(getResourceIdentifier()).append("\n");

        sb.append(super.toString());

        sb.append("end\n\n");

        return sb.toString();
    }

    protected final void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    ValueNode valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.getLine());

                    throw new BeamException(message);
                }

                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }
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

    public BeamCore getCore() {
        return core;
    }

    public void setCore(BeamCore core) {
        this.core = core;
    }

}
