package gyro.core.scope;

import gyro.lang.ast.Node;

public class DeferError extends Error {

    private final Node node;

    public DeferError(Node node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        return node.deferFailure();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
