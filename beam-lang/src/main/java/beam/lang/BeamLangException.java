package beam.lang;

public class BeamLangException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String code;

    public BeamLangException(String message) {
        super(message);
    }

    public BeamLangException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeamLangException(String message, Throwable cause, String code) {
        super(message, cause);

        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
