package gyro.lang;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class SyntaxErrorException extends RuntimeException {

    private final String file;
    private final List<SyntaxError> syntaxErrors;

    public SyntaxErrorException(String file, List<SyntaxError> syntaxErrors) {
        this.file = file;
        this.syntaxErrors = ImmutableList.copyOf(syntaxErrors);
    }

    public String getFile() {
        return file;
    }

    public List<SyntaxError> getSyntaxErrors() {
        return syntaxErrors;
    }

}
