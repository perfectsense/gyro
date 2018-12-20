package beam.lang;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.fusesource.jansi.AnsiRenderer;

import java.io.File;
import java.util.BitSet;
import java.util.List;

public class BeamErrorListener implements ANTLRErrorListener {

    private int syntaxErrors = 0;
    private String previousSource;

    public int getSyntaxErrors() {
        return syntaxErrors;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object symbol, int line, int column, String message, RecognitionException e) {
        if (e instanceof NoViableAltException) {
            return;
        }

        String filename = recognizer.getInputStream().getSourceName();
        filename = new File(filename).getName();

        if (!filename.equals(previousSource)) {
            System.out.println(AnsiRenderer.render(String.format("@|red Syntax errors found in %s:|@\n", filename)));
            previousSource = filename;
        }

        try {
            String error = underlineError(recognizer, (Token) symbol, line, column);

            List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
            String expected = expected(stack);

            if (!expected.isEmpty()) {
                System.err.println(AnsiRenderer.render(String.format("@|red on line %d: expected %s|@", line, expected)));
                System.err.println(AnsiRenderer.render(String.format("@|blue %s\n|@", error)));
            } else {
                System.err.println(AnsiRenderer.render(String.format("@|red on line %d:|@", line)));
                System.err.println(AnsiRenderer.render(String.format("@|blue %s\n|@", error)));
            }
        } catch (Exception ex) {
            // TODO: Handle failed render errors.
        }

        syntaxErrors++;
    }

    @Override
    public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {

    }

    private String expected(List<String> stack) {
        if (stack.isEmpty()) {
            return "";
        }

        switch (stack.get(0)) {
            case "value"      : return "a string, reference, boolean, number, list, or map";
            case "list_value" : return "a string or reference";
            default           : return "";
        }
    }

    private String underlineError(Recognizer recognizer, Token offendingToken, int line, int column) {
        StringBuilder sb = new StringBuilder();

        CommonTokenStream tokens = (CommonTokenStream)recognizer.getInputStream();
        String input = tokens.getTokenSource().getInputStream().toString();
        String[] lines = input.split("\n");

        String errorLine = lines[line - 1];
        sb.append(errorLine).append("\n");

        for (int i = 0; i < column; i++) {
            sb.append(" ");
        }

        int start = offendingToken.getStartIndex();
        int stop = offendingToken.getStopIndex();
        if (start >= 0 && stop >= 0) {
            for (int i = start; i <= stop; i++) {
                sb.append("^");
            }
        }

        return sb.toString();
    }

}
