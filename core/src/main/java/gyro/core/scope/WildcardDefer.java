package gyro.core.scope;

import gyro.lang.ast.Node;

class WildcardDefer extends Defer {

    public WildcardDefer(Node node, String type) {
        super(node, String.format("Can't resolve wildcard reference to @|bold %s|@ type!", type));
    }

}
