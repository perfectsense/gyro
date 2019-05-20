package gyro.core;

import java.util.concurrent.TimeUnit;

public class Wait {

    public static Waiter atMost(long duration, TimeUnit unit) {
        return new Waiter().atMost(duration, unit);
    }

    public static Waiter checkEvery(long duration, TimeUnit unit) {
        return new Waiter().checkEvery(duration, unit);
    }

    public static Waiter prompt(boolean prompt) {
        return new Waiter().prompt(prompt);
    }

    public static boolean until(WaitCheck check) {
        return new Waiter().until(check);
    }
}
