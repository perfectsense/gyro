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

    public void appendNewline() throws IOException {
        appendable.append('\n');

        for (int i = 0; i < indentDepth; i++) {
            appendable.append("    ");
        }
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return appendable.append(csq);
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        return appendable.append(csq, start, end);
    }

    @Override
    public Appendable append(char c) throws IOException {
        return appendable.append(c);
    }

}
