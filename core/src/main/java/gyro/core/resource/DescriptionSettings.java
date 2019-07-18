package gyro.core.resource;

import java.util.HashMap;
import java.util.Map;

import gyro.core.scope.Settings;
import gyro.lang.ast.Node;

public class DescriptionSettings extends Settings {

    private Map<String, Node> typeDescriptions;
    private Node description;

    public Map<String, Node> getTypeDescriptions() {
        if (typeDescriptions == null) {
            typeDescriptions = new HashMap<>();
        }

        return typeDescriptions;
    }

    public void setTypeDescriptions(Map<String, Node> typeDescriptions) {
        this.typeDescriptions = typeDescriptions;
    }

    public Node getDescription() {
        return description;
    }

    public void setDescription(Node description) {
        this.description = description;
    }

}
