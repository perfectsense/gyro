package beam.lang;

@FunctionalInterface
public interface ThrowingProcedure<E extends Throwable> {

    void execute() throws E;
}
