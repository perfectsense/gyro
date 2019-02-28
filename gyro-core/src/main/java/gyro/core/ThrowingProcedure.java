package gyro.core;

@FunctionalInterface
public interface ThrowingProcedure<E extends Throwable> {

    void execute() throws E;
}