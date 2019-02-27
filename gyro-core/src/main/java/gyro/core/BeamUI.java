package gyro.core;

public interface BeamUI {

    boolean isVerbose();

    void setVerbose(boolean verbose);

    boolean readBoolean(Boolean defaultValue, String message, Object... arguments);

    void readEnter(String message, Object... arguments);

    <E extends Enum<E>> E readNamedOption(E options);

    String readPassword(String message, Object... arguments);

    String readText(String message, Object... arguments);

    void indent();

    void unindent();

    default <E extends Throwable> void indented(ThrowingProcedure<E> procedure) throws E {
        indent();

        try {
            procedure.execute();

        } finally {
            unindent();
        }
    }

    void write(String message, Object... arguments);

    void writeError(Throwable error, String message, Object... arguments);

}
