package beam.lang.types;

import beam.core.diff.ResourceName;
import beam.lang.BeamFile;
import beam.lang.BeamLanguageException;
import beam.lang.BeamVisitor;
import beam.lang.Container;
import beam.lang.Node;
import beam.lang.Resource;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReferenceValue extends Value {

    private String type;
    private String name;
    private StringExpressionValue nameExpression;
    private String attribute;
    private Container referencedBlock;
    private Value value;

    public ReferenceValue(BeamParser.ReferenceBodyContext context) {
        // $(reference_type reference_name | reference_attribute)
        if (context.referenceType() != null) {
            this.type = context.referenceType().getText();
        }

        if (context.referenceName() != null) {
            if (context.referenceName().stringExpression() != null) {
                this.nameExpression = BeamVisitor.parseStringExpressionValue(context.referenceName().stringExpression());
                this.nameExpression.parent(this);
            } else {
                this.name = context.referenceName().getText();
            }
        }

        if (context.referenceAttribute() != null) {
            this.attribute = context.referenceAttribute().getText();
        }

        // $(reference_name)
        if (context.referenceType() != null && context.referenceName() == null) {
            this.name = context.referenceType().getText();
            this.type = null;
        }
    }

    public ReferenceValue(String name) {
        this(null, name, null);
    }

    public ReferenceValue(String type, String name) {
        this(type, name, null);
    }

    public ReferenceValue(String type, String name, String attribute) {
        this.type = type;
        this.name = name;
        this.attribute = attribute;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        if (nameExpression != null) {
            return nameExpression.getValue();
        }

        return name;
    }

    public List<String> getScopes() {
        if (getName() == null) {
            return new ArrayList<>();
        }

        return Arrays.asList(getName().split("\\."));
    }

    public String getAttribute() {
        return attribute;
    }

    public Container getReferencedContainer() {
        return referencedBlock;
    }

    public Value getReferenceValue() {
        return value;
    }

    public boolean isSimpleValue() {
        return getName() != null && getType() == null;
    }

    @Override
    public Object getValue() {
        if (getReferencedContainer() != null && getAttribute() == null) {
            return getReferencedContainer();
        } else if (getReferencedContainer() != null && getAttribute() != null) {
            return getReferencedContainer().resolvedKeyValues().get(getAttribute());
        }

        if (getReferenceValue() != null) {
            return getReferenceValue().getValue();
        }

        return null;
    }

    @Override
    public Value copy() {
        ReferenceValue reference = new ReferenceValue(getType(), getName(), getAttribute());
        reference.nameExpression = nameExpression != null ? nameExpression.copy() : null;
        reference.referencedBlock = referencedBlock;

        return reference;
    }

    public Resource getParentResource() {
        Node parent = parent();

        // Traverse up
        while (parent != null) {
            if (parent instanceof Resource) {
                // Skip subresources
                ResourceName name = parent.getClass().getAnnotation(ResourceName.class);
                if (name != null && name.parent().equals("")) {
                    return (Resource) parent;
                }
            }

            parent = parent.parent();
        }

        return null;
    }

    @Override
    public boolean resolve() {
        Node parent = parent();

        if (nameExpression != null) {
            nameExpression.resolve();
        }

        // Traverse up
        while (parent != null) {
            if (parent instanceof Container) {
                Container container = (Container) parent;

                // Look for key/value pairs.
                if (isSimpleValue()) {
                    value = container.get(name);
                    if (value != null && (value != this)) {
                        return true;
                    }
                }
            }

            if (parent instanceof BeamFile) {
                BeamFile container = (BeamFile) parent;
                String name = getName();

                // Resolve scopes
                if (getScopes().size() > 1) {
                    name = "";
                    for (String key : getScopes()) {
                        BeamFile scope = container.importFile(key);
                        if (scope != null) {
                            container = scope;
                        } else {
                            name += key;
                        }
                    }
                }

                // Look for resources.
                referencedBlock = container.resource(getType(), name);

                // Only ResourceBlocks have a dependency chain.
                if (referencedBlock != null  && getParentResource() != null) {
                    Resource parentRef = getParentResource();
                    Resource resourceRef = (Resource) referencedBlock;

                    if (parentRef != resourceRef) {
                        resourceRef.dependents().add(parentRef);
                        parentRef.dependencies().add(resourceRef);
                    }

                    return true;
                }

                // Look for key/value pairs.
                if (isSimpleValue()) {
                    value = container.get(name);
                    if (value != null) {
                        return true;
                    }
                }
            }

            parent = parent.parent();
        }

        throw new BeamLanguageException("Unable to resolve reference.", this);
    }

    @Override
    public String serialize(int indent) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("$(");
        if (getType() != null) {
            sb.append(getType()).append(" ");
        }
        sb.append(getName());

        if (getAttribute() != null) {
            sb.append(" | ").append(getAttribute());
        }

        sb.append(")");

        return sb.toString();
    }

}
