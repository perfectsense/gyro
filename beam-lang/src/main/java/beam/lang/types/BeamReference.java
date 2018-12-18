package beam.lang.types;

import beam.lang.BeamLanguageException;
import com.google.common.base.CaseFormat;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BeamReference extends BeamValue {

    private String type;
    private String name;
    private String attribute;
    private BeamBlock referencedBlock;

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

    public BeamBlock getReferencedBlock() {
        return referencedBlock;
    }

    @Override
    public Object getValue() {
        if (getReferencedBlock() != null && getAttribute() == null) {
            return getReferencedBlock();
        } else if (getReferencedBlock() != null && getAttribute() != null) {
            try {
                for (PropertyDescriptor p : Introspector.getBeanInfo(getReferencedBlock().getClass()).getPropertyDescriptors()) {
                    Method reader = p.getReadMethod();

                    if (reader != null) {
                        String key = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, reader.getName().substring(3));
                        if (key.equals(getAttribute())) {
                            return reader.invoke(getReferencedBlock());
                        }
                    }
                }

            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
                return null;
            }
        }

        return null;
    }

    public boolean resolve(ResourceBlock parentBlock) {
        BeamBlock parent = parentBlock;
        while (parent != null) {
            if (parent instanceof ContainerBlock) {
                ContainerBlock containerBlock = (ContainerBlock) parent;

                referencedBlock = containerBlock.get(getName(), getType());
                if (referencedBlock != null  && referencedBlock instanceof ResourceBlock) {
                    ResourceBlock ref = (ResourceBlock) referencedBlock;

                    ((ResourceBlock) referencedBlock).dependents().add(getParentBlock());
                    //System.out.println(String.format("--- RESOLVED: %s %s => %s %s",
                    //    parentBlock.getResourceType(), parentBlock.getResourceIdentifier(),
                    //    ref.getResourceType(), ref.getResourceIdentifier()));

                    parentBlock.dependencies().add(referencedBlock);

                    return true;
                }
            }

            parent = parent.getParentBlock();
        }

        throw new BeamLanguageException("Unable to resolve reference.", this);
    }

    @Override
    public boolean resolve() {
        return true;
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
