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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapWrapperTest {

    @Test
    void constructorNull() {
        assertThatNullPointerException().isThrownBy(() -> new MapWrapper<>(null));
    }

    @Nested
    class Wrap {

        Map<Object, Object> map;
        MapWrapper<Object, Object> wrapper;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void beforeEach() {
            map = mock(Map.class);
            wrapper = new MapWrapper<>(map);
        }

        @Test
        void clear() {
            wrapper.clear();

            verify(map).clear();
            verifyNoMoreInteractions(map);
        }

        @Test
        void containsKey() {
            Object key = new Object();

            wrapper.containsKey(key);

            verify(map).containsKey(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void containsValue() {
            Object value = new Object();

            wrapper.containsValue(value);

            verify(map).containsValue(value);
            verifyNoMoreInteractions(map);
        }

        @Test
        void entrySet() {
            wrapper.entrySet();

            verify(map).entrySet();
            verifyNoMoreInteractions(map);
        }

        @Test
        void keySet() {
            wrapper.keySet();

            verify(map).keySet();
            verifyNoMoreInteractions(map);
        }

        @Test
        void get() {
            Object key = new Object();

            wrapper.get(key);

            verify(map).get(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void isEmpty() {
            wrapper.isEmpty();

            verify(map).isEmpty();
            verifyNoMoreInteractions(map);
        }

        @Test
        void put() {
            String key = "foo";
            Object value = new Object();

            wrapper.put(key, value);

            verify(map).put(key, value);
            verifyNoMoreInteractions(map);
        }

        @Test
        @SuppressWarnings("unchecked")
        void putAll() {
            Map<String, Object> other = mock(Map.class);

            wrapper.putAll(other);

            verify(map).putAll(other);
            verifyNoMoreInteractions(other);
        }

        @Test
        void remove() {
            Object key = new Object();

            wrapper.remove(key);

            verify(map).remove(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void size() {
            wrapper.size();

            verify(map).size();
            verifyNoMoreInteractions(map);
        }

        @Test
        void values() {
            wrapper.values();

            verify(map).values();
            verifyNoMoreInteractions(map);
        }

    }

}
