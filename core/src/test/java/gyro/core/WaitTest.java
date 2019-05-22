package gyro.core;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WaitTest {

    @Test
    void atMost() {
        long startTime = System.currentTimeMillis();

        assertThat(Wait.atMost(1, TimeUnit.SECONDS)
            .checkEvery(100, TimeUnit.MILLISECONDS)
            .prompt(false)
            .until(() -> false)).isFalse();

        assertThat(System.currentTimeMillis() - startTime)
            .isGreaterThanOrEqualTo(TimeUnit.SECONDS.toMillis(1));
    }

    @Test
    void until() {
        assertThat(Wait.until(() -> true)).isTrue();
    }
}