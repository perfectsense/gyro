package gyro.core;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Waiter.class);

    private long atMost;
    private long checkEvery;
    private boolean prompt;

    Waiter() {
        atMost(10, TimeUnit.SECONDS);
        checkEvery(1, TimeUnit.SECONDS);
        prompt(true);
    }

    public Waiter atMost(long duration, TimeUnit unit) {
        this.atMost = unit.toMillis(duration);
        return this;
    }

    public Waiter checkEvery(long duration, TimeUnit unit) {
        this.checkEvery = unit.toMillis(duration);
        return this;
    }

    public Waiter prompt(boolean prompt) {
        this.prompt = prompt;
        return this;
    }

    public boolean until(WaitCheck check) {
        do {
            long startTime = System.currentTimeMillis();

            while (true) {
                try {
                    if (check.check()) {
                        return true;
                    }

                } catch (Throwable error) {
                    LOGGER.debug("Failed to check!", error);
                }

                try {
                    Thread.sleep(checkEvery);

                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (atMost < System.currentTimeMillis() - startTime) {
                    break;
                }
            }
        } while (prompt && GyroCore.ui().readBoolean(Boolean.TRUE, "\nWait for completion?"));

        return false;
    }
}
