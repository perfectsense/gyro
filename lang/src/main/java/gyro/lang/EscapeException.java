package gyro.lang;

import org.antlr.v4.runtime.Token;

public class EscapeException extends RuntimeException {

    private final String escape;
    private final Token token;

    public EscapeException(String escape, Token token) {
        this.escape = escape;
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String getMessage() {
        return String.format("@|bold %s|@ isn't a valid escape character!", escape);
    }

}
