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

package gyro.lang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GyroCharStreamTest {

    static final String TEXT = "foo\nbar\rqux\r\nxyzzy";

    abstract class Base {

        GyroCharStream stream;

        @Test
        void getLineText() {
            assertThat(stream.getLineText(0)).isEqualTo("foo");
            assertThat(stream.getLineText(1)).isEqualTo("bar");
            assertThat(stream.getLineText(2)).isEqualTo("qux");
            assertThat(stream.getLineText(3)).isEqualTo("xyzzy");
        }

    }

    @Nested
    class WithInputStream extends Base {

        @BeforeEach
        void beforeEach() throws IOException {
            stream = new GyroCharStream(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)), "foo");
        }

        @Test
        void getSourceName() {
            assertThat(stream.getSourceName()).isEqualTo("foo");
        }

    }

    @Nested
    class WithString extends Base {

        @BeforeEach
        void beforeEach() {
            stream = new GyroCharStream(TEXT);
        }

        @Test
        void getSourceName() {
            assertThat(stream.getSourceName()).isEqualTo(IntStream.UNKNOWN_SOURCE_NAME);
        }

    }

}