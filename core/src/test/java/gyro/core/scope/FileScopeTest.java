package gyro.core.scope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileScopeTest {

    @Test
    void constructorNullFile() {
        assertThatNullPointerException()
            .isThrownBy(() -> new FileScope(mock(RootScope.class), null));
    }

    @Test
    void getFile() {
        assertThat(new FileScope(mock(RootScope.class), "foo").getFile()).isEqualTo("foo");
    }

}