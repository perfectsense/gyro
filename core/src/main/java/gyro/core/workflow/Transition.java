package gyro.core.workflow;

import gyro.core.resource.Scope;

public class Transition {

    private final String name;
    private final String to;
    private final String description;

    public Transition(String name, Scope scope) {
        this.name = name;
        this.to = (String) scope.get("to");
        this.description = (String) scope.get("description");
    }

    public String getTo() {
        return to;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}
