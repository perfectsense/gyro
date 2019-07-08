package gyro.core;

import gyro.lang.ast.Node;

public class GyroException extends RuntimeException {

    private final Node node;

    public GyroException(Node node, String message, Throwable cause) {
        super(message, cause);
        this.node = node;
    }

    public GyroException(Node node, String message) {
        this(node, message, null);
    }

    public GyroException(Node node, Throwable cause) {
        this(node, null, cause);
    }

    public GyroException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public GyroException(String message) {
        this(null, message, null);
    }

    public GyroException(Throwable cause) {
        this(null, null, cause);
    }

    public Node getNode() {
        return node;
    }

}
