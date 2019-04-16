package gyro.core.validation;

import java.util.ArrayList;
import java.util.List;

public class RangesValidator extends AbstractValidator<Ranges> {
    @Override
    boolean validate(Object value) {
        RangeValidator validator = new RangeValidator();

        for (Range range : annotation.value()) {
            if (!validator.isValid(range, value)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getMessage() {
        List<String> rangesString = new ArrayList<>();

        for (Range range : annotation.value()) {
            rangesString.add(String.format("[%s - %s]", range.low(), range.high()));
        }

        return String.format("Valid number should be in one of these ranges %s.", String.join(", ", rangesString));
    }
}