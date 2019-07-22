package gyro.core.validation;

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;

public class ValidationErrorException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationErrorException(List<ValidationError> errors) {
        this.errors = ImmutableList.copyOf(errors);
    }

    public void write(GyroUI ui) {
        ui.write("@|red %d validation errors!|@\n", errors.size());

        for (ValidationError e : errors) {
            e.write(ui);
        }
    }

}
