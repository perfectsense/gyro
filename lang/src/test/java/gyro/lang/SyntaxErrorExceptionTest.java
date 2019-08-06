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