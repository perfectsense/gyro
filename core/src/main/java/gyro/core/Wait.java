package gyro.core;

import java.util.concurrent.TimeUnit;

public class Wait {

    public static WaitBuilder atMost(long duration, TimeUnit unit) {
        return new WaitBuilder().atMost(duration, unit);
    }

    public static WaitBuilder checkEvery(long duration, TimeUnit unit) {
        return new WaitBuilder().checkEvery(duration, unit);
    }

    public static WaitBuilder prompt(boolean prompt) {
        return new WaitBuilder().prompt(prompt);
    }

    public static void until(WaitCheck check) {
        new WaitBuilder().until(check);
    }
}
