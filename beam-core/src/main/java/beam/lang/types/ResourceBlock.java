package beam.lang.types;

import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

public class ResourceBlock extends ContainerBlock {

    private String type;
    private String name;

    private Set<ResourceBlock> dependencies;
    private Set<ResourceBlock> dependents;

    public Set<ResourceBlock> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<ResourceBlock> dependents() {
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

        for (ResourceBlock resourceBlock : resources()) {
            for (String line : resourceBlock.toString().split("\n")) {
                sb.append("    " + line + "\n");
            }
        }

        for (String key : keys()) {
            String value = get(key).toString();

            if (value != null) {
                sb.append("    " + key + ": ");
                sb.append(get(key)).append("\n");
            }
        }

        sb.append("end\n\n");

        return sb.toString();
    }

    private void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                // Ignoring errors from setProperty
            }
        }
    }

}
