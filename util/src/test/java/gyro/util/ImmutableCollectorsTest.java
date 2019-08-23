package gyro.util;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Functions;
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

    @Test
    void toMap() {
        String item0 = "foo";
        String item1 = "bar";
        Map<String, String> map = Stream.of(item0, item1).collect(ImmutableCollectors.toMap(Functions.identity()));

        assertThat(map).containsExactly(
            new AbstractMap.SimpleImmutableEntry<>(item0, item0),
            new AbstractMap.SimpleImmutableEntry<>(item1, item1));
    }

    @Test
    void toMapImmutable() {
        Map<String, String> map = Stream.<String>empty().collect(ImmutableCollectors.toMap(Function.identity()));

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> map.put("foo", "foo"));
    }

    @Test
    void toMapParallel() {
        String item0 = "foo";
        String item1 = "bar";

        Map<String, String> map = Arrays.asList(item0, item1)
            .parallelStream()
            .collect(ImmutableCollectors.toMap(Functions.identity()));

        assertThat(map).containsExactly(
            new AbstractMap.SimpleImmutableEntry<>(item0, item0),
            new AbstractMap.SimpleImmutableEntry<>(item1, item1));
    }

}