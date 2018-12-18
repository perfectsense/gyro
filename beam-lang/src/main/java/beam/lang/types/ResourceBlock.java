package beam.lang.types;

import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

public class ResourceBlock extends ContainerBlock {

    private String type;
    private String name;

    private Set<BeamBlock> dependencies;
    private Set<BeamBlock> dependents;

    public Set<BeamBlock> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<BeamBlock> dependents() {
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

    @Override
    public boolean resolve() {
        boolean resolved = super.resolve();

        if (resolved) {
            for (BeamBlock child : getBlocks()) {
                if (child instanceof KeyValueBlock) {
                    KeyValueBlock keyValueBlock = (KeyValueBlock) child;

                    String key = keyValueBlock.getKey();
                    Object value = keyValueBlock.getValue().getValue();

                    try {
                        String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
                        BeanUtils.setProperty(this, convertedKey, value);
                    } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                        // Ignoring errors from setProperty
                    }
                }
            }
        }

        return resolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getResourceType()).append(" ");
        sb.append(getResourceIdentifier()).append("\n");

        for (BeamBlock block : getBlocks()) {
            for (String line : block.toString().split("\n")) {
                sb.append("    " + line + "\n");
            }
        }

        sb.append("end\n\n");

        return sb.toString();
    }

}
