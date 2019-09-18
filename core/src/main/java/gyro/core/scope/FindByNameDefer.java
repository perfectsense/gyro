package gyro.core.scope;

import gyro.lang.ast.Node;

class FindByNameDefer extends Defer {

    private final String id;

    public FindByNameDefer(Node node, String type, String name) {
        super(node, String.format("Can't find @|bold %s|@ resource of @|bold %s|@ type!", name, type));

        this.id = type + "::" + name;
    }

    public String getId() {
        return id;
    }

}
