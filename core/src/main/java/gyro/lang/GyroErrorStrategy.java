package gyro.lang;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;

public class GyroErrorStrategy extends DefaultErrorStrategy {

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        recognizer.notifyErrorListeners(e.getOffendingToken(), "invalid input", e);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
        String msg = "expecting " + e.getExpectedTokens().toString(recognizer.getVocabulary());
        recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        Token t = recognizer.getCurrentToken();
        String tokenName = getTokenErrorDisplay(t);
        String msg = "extra input " + tokenName;
        recognizer.notifyErrorListeners(t, msg, null);
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        Token t = recognizer.getCurrentToken();
        IntervalSet expecting = getExpectedTokens(recognizer);
        String msg = "missing " + expecting.toString(recognizer.getVocabulary());

        recognizer.notifyErrorListeners(t, msg, null);
    }
}
