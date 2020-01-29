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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyntaxErrorExceptionTest {

    @Test
    void constructorNullFile() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new SyntaxErrorException(null, new ArrayList<>()));
    }

    @Test
    void constructorNullErrors() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new SyntaxErrorException("foo", null));
    }

    @Test
    void getFile() {
        SyntaxErrorException exception = new SyntaxErrorException("foo", new ArrayList<>());

        assertThat(exception.getFile()).isEqualTo("foo");
    }

    @Nested
    class GetSyntaxErrors {

        List<SyntaxError> errors;
        SyntaxErrorException exception;

        @BeforeEach
        void beforeEach() {
            errors = Arrays.asList(mock(SyntaxError.class), mock(SyntaxError.class));
            exception = new SyntaxErrorException("foo", errors);
        }

        @Test
        void equality() {
            assertThat(exception.getErrors()).isEqualTo(errors);
        }

        @Test
        void copying() {
            assertThat(exception.getErrors()).isNotSameAs(errors);
        }

        @Test
        void immutability() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> exception.getErrors().add(mock(SyntaxError.class)));
        }

    }

}