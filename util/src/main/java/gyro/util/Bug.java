package gyro.util;

public class Bug extends Error {

    public Bug(String message) {
        super(message);
    }

    public Bug(Throwable cause) {
        super(cause);
    }

}
