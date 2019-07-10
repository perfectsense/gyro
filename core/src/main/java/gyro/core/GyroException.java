package gyro.core;

import gyro.lang.Locatable;

public class GyroException extends RuntimeException {

    private final Locatable locatable;

    public GyroException(Locatable locatable, String message, Throwable cause) {
        super(message, cause);
        this.locatable = locatable;
    }

    public GyroException(Locatable locatable, String message) {
        this(locatable, message, null);
    }

    public GyroException(Locatable locatable, Throwable cause) {
        this(locatable, null, cause);
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

    public Locatable getLocatable() {
        return locatable;
    }

}
