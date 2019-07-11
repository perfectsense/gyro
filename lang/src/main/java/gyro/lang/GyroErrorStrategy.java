package gyro.lang;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;

public class GyroErrorStrategy extends DefaultErrorStrategy {

    public static final GyroErrorStrategy INSTANCE = new GyroErrorStrategy();

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException error) {
        recognizer.notifyErrorListeners(
            error.getOffendingToken(),
            "Expected " + error.getExpectedTokens().toString(recognizer.getVocabulary()),
            error);
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        recognizer.notifyErrorListeners(
            recognizer.getCurrentToken(),
            "Missing " + getExpectedTokens(recognizer).toString(recognizer.getVocabulary()),
            null);
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException error) {
        recognizer.notifyErrorListeners(error.getOffendingToken(), "Invalid input", error);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(recognizer.getCurrentToken(), "Extra input", null);
    }

}
