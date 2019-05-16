package gyro.core;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WaitTest {

    @Test
    void atMost() {
        long startTime = System.currentTimeMillis();

        assertThat(Wait.atMost(10, TimeUnit.SECONDS)
            .prompt(false)
            .until(() -> false)).isFalse();

        assertThat(System.currentTimeMillis() - startTime)
            .isGreaterThanOrEqualTo(TimeUnit.SECONDS.toMillis(10));
    }

    @Test
    void until() {
        assertThat(Wait.until(() -> true)).isTrue();
    }
}