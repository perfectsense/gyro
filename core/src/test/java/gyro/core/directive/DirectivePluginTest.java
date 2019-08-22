package gyro.core.directive;

import gyro.core.FileBackend;
import gyro.core.scope.RootScope;
import gyro.util.Bug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectivePluginTest {

    RootScope root;

    @BeforeEach
    void beforeEach() {
        root = new RootScope("", mock(FileBackend.class), null, null);
    }

    @Test
    void onEachClassPrivateDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(root, PrivateDirectiveProcessor.class));
    }

    @Test
    void onEachClassNoNullaryDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(root, NoNullaryDirectiveProcessor.class));
    }

    @Test
    void onEachClass() {
        new DirectivePlugin().onEachClass(root, TestDirectiveProcessor.class);

        assertThat(root.getSettings(DirectiveSettings.class).getProcessor("test"))
            .isInstanceOf(TestDirectiveProcessor.class);
    }

}
