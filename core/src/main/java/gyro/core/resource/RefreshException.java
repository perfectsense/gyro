package gyro.core.resource;

public class RefreshException extends RuntimeException {

    private Resource resource;

    public RefreshException(Resource resource) {
        this.resource = resource;
    }

    public RefreshException(String message, Resource resource) {
        super(message);
        this.resource = resource;
    }

    public RefreshException(String message, Throwable cause, Resource resource) {
        super(message, cause);
        this.resource = resource;
    }

    public RefreshException(Throwable cause, Resource resource) {
        super(cause);
        this.resource = resource;
    }

    public RefreshException(
        String message,
        Throwable cause,
        boolean enableSuppression,
        boolean writableStackTrace,
        Resource resource) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }
}
