/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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