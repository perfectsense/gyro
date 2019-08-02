package gyro.lang.ast;

import gyro.lang.GyroCharStream;
import gyro.lang.Locatable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class Rule implements Locatable {

    protected final Token start;
    protected final Token stop;

    public Rule(Token start, Token stop) {
        this.start = start;
        this.stop = stop;
    }

    public Rule(ParserRuleContext context) {
        if (context != null) {
            start = context.getStart();
            stop = context.getStop();

        } else {
            start = null;
            stop = null;
        }
    }

    @Override
    public GyroCharStream getStream() {
        return start != null ? (GyroCharStream) start.getTokenSource().getInputStream() : null;
    }

    @Override
    public int getStartLine() {
        return start != null ? start.getLine() - 1 : -1;
    }

    @Override
    public int getStartColumn() {
        return start != null ? start.getCharPositionInLine() : -1;
    }

    @Override
    public int getStopLine() {
        return stop != null ? stop.getLine() - 1 : -1;
    }

    @Override
    public int getStopColumn() {
        if (stop == null) {
            return -1;
        }

        int column = stop.getCharPositionInLine();
        int startIndex = stop.getStartIndex();
        int stopIndex = stop.getStopIndex();

        if (startIndex >= 0 && stopIndex >= 0 && stopIndex > startIndex) {
            column += stopIndex - startIndex;
        }

        return column;
    }

}
