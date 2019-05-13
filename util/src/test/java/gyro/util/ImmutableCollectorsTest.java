package gyro.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ImmutableCollectorsTest {

    @Test
    void toList() {
        String item0 = "foo";
        String item1 = "bar";
        List<String> list = Stream.of(item0, item1).collect(ImmutableCollectors.toList());

        assertThat(list).containsExactly(item0, item1);
    }

    @Test
    void toListImmutable() {
        List<String> list = Stream.<String>empty().collect(ImmutableCollectors.toList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> list.add("foo"));
    }

    @Test
    void toListParallel() {
        String item0 = "foo";
        String item1 = "bar";

        List<String> list = Arrays.asList(item0, item1)
            .parallelStream()
            .collect(ImmutableCollectors.toList());

        assertThat(list).containsExactly(item0, item1);
    }

}