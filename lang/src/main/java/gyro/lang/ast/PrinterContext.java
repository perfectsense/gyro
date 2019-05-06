package gyro.lang.ast;

import java.io.IOException;

public class PrinterContext implements Appendable {

    private final Appendable appendable;
    private final int indentDepth;

    public PrinterContext(Appendable appendable, int indentDepth) {
        this.appendable = appendable;
        this.indentDepth = indentDepth;
    }

    public PrinterContext indented() {
        return new PrinterContext(appendable, indentDepth + 1);
    }

    public void appendNewline() {
        try {
            appendable.append('\n');

            for (int i = 0; i < indentDepth; i++) {
                appendable.append("    ");
            }

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public Appendable append(CharSequence csq) {
        try {
            return appendable.append(csq);

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        try {
            return appendable.append(csq, start, end);

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public Appendable append(char c) {
        try {
            return appendable.append(c);

        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

}
