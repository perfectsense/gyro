package beam.lang;

import beam.core.diff.ResourceName;
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

    ReferenceValue(BeamParser.Reference_bodyContext context) {
        // $(reference_type reference_name | reference_attribute)
        if (context.reference_type() != null) {
            this.type = context.reference_type().getText();
        }

        if (context.reference_name() != null) {
            if (context.reference_name().string_expression() != null) {
                this.nameExpression = BeamVisitor.parseStringExpressionValue(context.reference_name().string_expression());
                this.nameExpression.parentNode(this);
            } else {
                this.name = context.reference_name().getText();
            }
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

    public Container getReferencedBlock() {
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
        if (getReferencedBlock() != null && getAttribute() == null) {
            return getReferencedBlock();
        } else if (getReferencedBlock() != null && getAttribute() != null) {
            return getReferencedBlock().resolvedKeyValues().get(getAttribute());
        }

        if (getReferenceValue() != null) {
            return getReferenceValue().getValue();
        }

        return null;
    }

    @Override
    public Value copy() {
        ReferenceValue referenceNode = new ReferenceValue(getType(), getName(), getAttribute());
        referenceNode.nameExpression = nameExpression != null ? nameExpression.copy() : null;
        referenceNode.referencedBlock = referencedBlock;

        return referenceNode;
    }

    public Resource getParentResourceNode() {
        Node parent = parentNode();

        // Traverse up
        while (parent != null) {
            if (parent instanceof Resource) {
                // Skip subresources
                ResourceName name = parent.getClass().getAnnotation(ResourceName.class);
                if (name != null && name.parent().equals("")) {
                    return (Resource) parent;
                }
            }

            parent = parent.parentNode();
        }

        return null;
    }

    @Override
    public boolean resolve() {
        Node parent = parentNode();

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
                    if (value != null) {
                        return true;
                    }
                }
            }

            if (parent instanceof BeamFile) {
                BeamFile containerNode = (BeamFile) parent;
                String name = getName();

                // Resolve scopes
                if (getScopes().size() > 1) {
                    name = "";
                    for (String key : getScopes()) {
                        BeamFile scope = containerNode.importFile(key);
                        if (scope != null) {
                            containerNode = scope;
                        } else {
                            name += key;
                        }
                    }
                }

                // Look for resources.
                referencedBlock = containerNode.resource(getType(), name);

                // Only ResourceBlocks have a dependency chain.
                if (referencedBlock != null  && getParentResourceNode() != null) {
                    Resource parentRef = getParentResourceNode();
                    Resource resourceRef = (Resource) referencedBlock;

                    if (parentRef != resourceRef) {
                        resourceRef.dependents().add(parentRef);
                        parentRef.dependencies().add(resourceRef);
                    }

                    return true;
                }

                // Look for key/value pairs.
                if (isSimpleValue()) {
                    value = containerNode.get(name);
                    if (value != null) {
                        return true;
                    }
                }
            }

            parent = parent.parentNode();
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
