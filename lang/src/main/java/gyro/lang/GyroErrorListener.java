package gyro.lang;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class GyroErrorListener extends BaseErrorListener {

    private final GyroCharStream stream;
    private final List<SyntaxError> syntaxErrors = new ArrayList<>();

    public GyroErrorListener(GyroCharStream stream) {
        this.stream = stream;
    }

    public List<SyntaxError> getSyntaxErrors() {
        return syntaxErrors;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer,
        Object symbol,
        int line,
        int column,
        String message,
        RecognitionException error) {

        syntaxErrors.add(symbol instanceof Token
            ? new SyntaxError(stream, message, (Token) symbol)
            : new SyntaxError(stream, message, line - 1, column));
    }

}
