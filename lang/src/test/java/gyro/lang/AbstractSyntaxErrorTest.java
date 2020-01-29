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

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

abstract class AbstractSyntaxErrorTest {

    static final String MESSAGE = "foo";

    GyroCharStream stream;

    @BeforeEach
    void beforeEach() {
        stream = new GyroCharStream("bar");
    }

    void assertSyntaxError(
        SyntaxError error,
        int startLine,
        int startColumn,
        int stopLine,
        int stopColumn) {

        assertThat(error.getMessage()).isEqualTo(MESSAGE);
        assertThat(error.getStream()).isEqualTo(stream);
        assertThat(error.getStartLine()).isEqualTo(startLine);
        assertThat(error.getStartColumn()).isEqualTo(startColumn);
        assertThat(error.getStopLine()).isEqualTo(stopLine);
        assertThat(error.getStopColumn()).isEqualTo(stopColumn);
    }

}
