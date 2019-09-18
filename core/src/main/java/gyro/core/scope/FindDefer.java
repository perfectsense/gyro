package gyro.core.scope;

import gyro.lang.ast.Node;

class FindDefer extends Defer {

    private final String key;

    public FindDefer(Node node, String type, String name) {
        super(node, String.format("Can't find @|bold %s|@ resource of @|bold %s|@ type!", name, type));

        this.key = type + "::" + name;
    }

    public String getKey() {
        return key;
    }

}
