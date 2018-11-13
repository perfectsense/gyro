package beam.core;

@FunctionalInterface
public interface ThrowingProcedure<E extends Throwable> {

    void execute() throws E;
}