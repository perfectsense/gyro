package gyro.lang;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class SyntaxErrorException extends RuntimeException {

    private final String file;
    private final List<SyntaxError> errors;

    public SyntaxErrorException(String file, List<SyntaxError> errors) {
        this.file = Preconditions.checkNotNull(file);
        this.errors = ImmutableList.copyOf(Preconditions.checkNotNull(errors));
    }

    public String getFile() {
        return file;
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

}
