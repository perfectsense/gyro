package beam.lang;

import beam.lang.types.BeamReferable;

public class BeamLanguageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private BeamReferable referable;

    public BeamLanguageException(String message) {
        super(message);
    }

    public BeamLanguageException(String message, BeamReferable referable) {
        super(message);
        this.referable = referable;
    }

    public BeamLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeamLanguageException(String message, Throwable cause, BeamReferable referable) {
        super(message, cause);
        this.referable = referable;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();

        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");
        if (getReferable() != null) {
            sb.append("    at line ");
            sb.append(getReferable().getLine()).append(":");
            sb.append(getReferable().getColumn()).append(" => ");
            sb.append(getReferable().toString());
        }

        return sb.toString();
    }

    public BeamReferable getReferable() {
        return referable;
    }

}
