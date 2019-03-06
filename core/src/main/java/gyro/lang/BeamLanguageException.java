package gyro.lang;

public class BeamLanguageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BeamLanguageException(String message) {
        super(message);
    }

    public BeamLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();

        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");

        return sb.toString();
    }

}
