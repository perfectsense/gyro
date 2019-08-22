package gyro.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ImmutableCollectors {

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collector.of(
            ImmutableList::<T>builder,
            ImmutableList.Builder::add,
            (x, y) -> {
                x.addAll(y.build());
                return x;
            },
            ImmutableList.Builder::build);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends V> valueMapper) {

        return Collector.of(
            ImmutableMap::<K, V>builder,
            (map, item) -> map.put(keyMapper.apply(item), valueMapper.apply(item)),
            (x, y) -> {
                x.putAll(y.build());
                return x;
            },
            ImmutableMap.Builder::build);
    }

    public static <T, K> Collector<T, ?, Map<K, T>> toMap(Function<? super T, ? extends K> keyMapper) {
        return toMap(keyMapper, Functions.identity());
    }

}
