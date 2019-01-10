package beam.lang;

import beam.core.BeamResource;

import java.util.ArrayList;
import java.util.List;

public class ForControl extends ControlStructure {

    private List<String> variables;
    private List<ValueNode> listValues;

    public List<String> variables() {
        if (variables == null) {
            variables = new ArrayList<>();
        }

        return variables;
    }

    public void variables(List<String> variables) {
        this.variables = variables;
    }

    public List<ValueNode> listValues() {
        if (listValues == null) {
            listValues = new ArrayList<>();
        }

        return listValues;
    }

    public void listValues(List<ValueNode> listValues) {
        this.listValues = listValues;
    }

    @Override
    public boolean resolve() {
        for (ValueNode value : listValues()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        return super.resolve();
    }

    @Override
    public void evaluate() {
        // Validate there are enough values to evenly loop over the list.
        if (listValues().size() % variables().size() != 0) {
            throw new BeamLanguageException("Not enough values to loop", this);
        }

        int loops = listValues().size() / variables().size();

        Node parent = parentNode();
        for (int i = 0; i < loops; i++) {
            Container scope = new Container();
            scope.setParentNode(parent);

            for (int j = 0; j < variables().size(); j++) {
                int index = (i * variables().size()) + j;

                String variableName = variables().get(index % variables.size());
                ValueNode valueNode = listValues().get(index);

                scope.put(variableName, valueNode);
            }

            // Duplicate key/values
            if (parent instanceof Container) {

                Container container = (Container) parent;
                for (String key : keys()) {

                    ValueNode valueNode = get(key).copy();
                    valueNode.setParentNode(scope);

                    container.put(key, valueNode);
                }
            }

            // Copy resource nodes
            if (parent instanceof ResourceContainer) {
                ResourceContainer resourceContainerNode = (ResourceContainer) parent;

                for (BeamResource resourceNode : resources()) {
                    BeamResource copy = resourceNode.copy();
                    copy.setParentNode(scope);

                    resourceContainerNode.putResourceKeepParent(copy);
                }
            }

            // Copy subresource nodes
            if (parent instanceof BeamResource) {
                BeamResource resource = (BeamResource) parent;

                for (String fieldName : subResources().keySet()) {
                    List<BeamResource> subresources = subResources().get(fieldName);

                    for (BeamResource subresource : subresources) {
                        BeamResource copy = subresource.copy();
                        copy.setParentNode(scope);

                        resource.putSubresource(fieldName, copy);
                    }
                }

                //resourceNode.syncInternalToProperties();
                resource.resolve();
            }
        }
    }

}

