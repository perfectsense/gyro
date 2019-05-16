package gyro.core;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WaitTest {

    @Test
    void atMost() {
        long startTime = System.currentTimeMillis();

        Wait.atMost(10, TimeUnit.SECONDS)
            .prompt(false)
            .until(() -> false);

        assertThat(System.currentTimeMillis() - startTime)
            .isGreaterThanOrEqualTo(TimeUnit.SECONDS.toMillis(10));
    }
}