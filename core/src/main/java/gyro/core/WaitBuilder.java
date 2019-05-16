package gyro.core;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitBuilder.class);

    private long atMost;
    private long checkEvery;
    private boolean prompt;

    public WaitBuilder() {
        atMost(10, TimeUnit.SECONDS);
        checkEvery(1, TimeUnit.SECONDS);
        prompt(true);
    }

    public WaitBuilder atMost(long duration, TimeUnit unit) {
        this.atMost = unit.toMillis(duration);
        return this;
    }

    public WaitBuilder checkEvery(long duration, TimeUnit unit) {
        this.checkEvery = unit.toMillis(duration);
        return this;
    }

    public WaitBuilder prompt(boolean prompt) {
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
        } while (prompt && GyroCore.ui().readBoolean(Boolean.FALSE, "\nWait for completion?"));

        return false;
    }
}
