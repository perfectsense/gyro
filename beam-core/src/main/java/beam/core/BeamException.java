package beam.core;

public class BeamException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String code;

    public BeamException(String message) {
        super(message);
    }

    public BeamException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeamException(String message, Throwable cause, String code) {
        super(message, cause);

        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
