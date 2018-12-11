package beam.core;

import java.util.Set;
import java.util.TreeSet;

public class BeamValidationException extends BeamException {

    private final Set<String> validationErrors = new TreeSet<>();

    public BeamValidationException(String message) {
        super(message);
    }

    public boolean isThrowing() {
        return !validationErrors.isEmpty();
    }

    public void addValidationError(String message) {
        validationErrors.add(message);
    }

    @Override
    public String getMessage() {
        return String.format("%s\n%s", super.getMessage(), String.join("\n\n", validationErrors));
    }
}
