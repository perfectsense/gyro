package gyro.core;

public class GyroException extends RuntimeException {

    public GyroException(String message) {
        super(message);
    }

    public GyroException(String message, Throwable cause) {
        super(message, cause);
    }

}
