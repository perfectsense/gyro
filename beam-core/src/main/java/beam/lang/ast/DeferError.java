package beam.lang.ast;

class DeferError extends Error {

    private final Node node;

    public DeferError(Node node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        return node.toString();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
