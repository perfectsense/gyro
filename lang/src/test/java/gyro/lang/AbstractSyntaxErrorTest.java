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
