package gyro.core;

public class GyroException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String code;

    public GyroException(String message) {
        super(message);
    }

    public GyroException(String message, Throwable cause) {
        super(message, cause);
    }

    public GyroException(String message, Throwable cause, String code) {
        super(message, cause);

        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
