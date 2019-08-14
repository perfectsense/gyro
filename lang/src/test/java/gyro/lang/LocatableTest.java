package gyro.lang;

import org.antlr.v4.runtime.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LocatableTest {

    GyroCharStream stream;

    @BeforeEach
    void beforeEach() {
        stream = new GyroCharStream("foo\nbar\nqux\nxyzzy");
    }

    @Test
    void getFile() {
        assertThat(new TestLocatable(stream, 0, 0, 0, 0).getFile())
            .isEqualTo(IntStream.UNKNOWN_SOURCE_NAME);
    }

    @Test
    void toLocationNull() {
        assertThat(new TestLocatable(null, 0, 0, 0, 0).toLocation()).isNull();
    }

    @Test
    void toLocationSameLineColumn() {
        assertThat(new TestLocatable(stream, 10, 20, 10, 20).toLocation())
            .isEqualTo("on line @|bold 11|@ at column @|bold 21|@");
    }

    @Test
    void toLocationSameLine() {
        assertThat(new TestLocatable(stream, 10, 20, 10, 30).toLocation())
            .isEqualTo("on line @|bold 11|@ from column @|bold 21|@ to @|bold 31|@");
    }

    @Test
    void toLocation() {
        assertThat(new TestLocatable(stream, 10, 20, 30, 40).toLocation())
            .isEqualTo("from line @|bold 11|@ at column @|bold 21|@ to line @|bold 31|@ at column @|bold 41|@");
    }

    @Test
    void toCodeSnippetNull() {
        assertThat(new TestLocatable(null, 0, 0, 0, 0).toCodeSnippet()).isNull();
    }

    @Test
    void toCodeSnippetFirst() {
        assertThat(new TestLocatable(stream, 0, 0, 0, 2).toCodeSnippet())
            .isEqualTo("1: @|red,underline foo|@\n2: bar\n");
    }

    @Test
    void toCodeSnippetLast() {
        assertThat(new TestLocatable(stream, 3, 0, 3, 4).toCodeSnippet())
            .isEqualTo("3: qux\n4: @|red,underline xyzzy|@\n");
    }

    @Test
    void toCodeSnippetFull() {
        assertThat(new TestLocatable(stream, 1, 0, 1, 2).toCodeSnippet())
            .isEqualTo("1: foo\n2: @|red,underline bar|@\n3: qux\n");
    }

    @Test
    void toCodeSnippet() {
        assertThat(new TestLocatable(stream, 1, 1, 1, 1).toCodeSnippet())
            .isEqualTo("1: foo\n2: b@|red,underline a|@r\n3: qux\n");
    }

}