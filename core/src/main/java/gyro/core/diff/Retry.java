package gyro.core.diff;

public class Retry extends Error {

    public static final Retry INSTANCE = new Retry();

    private Retry() {
    }

}
