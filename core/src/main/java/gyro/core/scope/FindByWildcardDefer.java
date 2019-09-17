package gyro.core.scope;

import gyro.lang.ast.Node;

class FindByWildcardDefer extends Defer {

    public FindByWildcardDefer(Node node, String type) {
        super(node, String.format("Can't resolve wildcard reference to @|bold %s|@ type!", type));
    }

}
