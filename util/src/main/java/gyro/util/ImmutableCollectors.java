package gyro.util;

import java.util.List;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;

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

}
