package beam.lang;

import java.util.ArrayList;
import java.util.List;

public class ForControl extends ControlStructure {

    private List<String> variables;
    private List<Value> listValues;

    public List<String> variables() {
        if (variables == null) {
            variables = new ArrayList<>();
        }

        return variables;
    }

    public void variables(List<String> variables) {
        this.variables = variables;
    }

    public List<Value> listValues() {
        if (listValues == null) {
            listValues = new ArrayList<>();
        }

        return listValues;
    }

    public void listValues(List<Value> listValues) {
        this.listValues = listValues;
    }

    @Override
    public boolean resolve() {
        for (Value value : listValues()) {
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
            scope.parentNode(parent);

            for (int j = 0; j < variables().size(); j++) {
                int index = (i * variables().size()) + j;

                String variableName = variables().get(index % variables.size());
                Value value = listValues().get(index);

                scope.put(variableName, value);
            }

            // Duplicate key/values
            if (parent instanceof Container) {

                Container container = (Container) parent;
                for (String key : keys()) {

                    Value value = get(key).copy();
                    value.parentNode(scope);

                    container.put(key, value);
                }
            }

            // Copy resource nodes
            if (parent instanceof ResourceContainer) {
                ResourceContainer resourceContainerNode = (ResourceContainer) parent;

                for (Resource resourceNode : resources()) {
                    Resource copy = resourceNode.copy();
                    copy.parentNode(scope);

                    resourceContainerNode.putResourceKeepParent(copy);
                }
            }

            // Copy subresource nodes
            if (parent instanceof Resource) {
                Resource resource = (Resource) parent;

                for (String fieldName : subResources().keySet()) {
                    List<Resource> subresources = subResources().get(fieldName);

                    for (Resource subresource : subresources) {
                        Resource copy = subresource.copy();
                        copy.parentNode(scope);

                        resource.putSubresource(fieldName, copy);
                    }
                }

                //resourceNode.syncInternalToProperties();
                resource.resolve();
            }
        }
    }

}

