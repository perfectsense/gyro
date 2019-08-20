package gyro.core.directive;

import gyro.core.FileBackend;
import gyro.core.scope.RootScope;
import gyro.util.Bug;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectivePluginTest {

    @Test
    void onEachClassNotDirectiveProcessor() {
        RootScope root = mock(RootScope.class);

        new DirectivePlugin().onEachClass(root, getClass());

        verifyNoMoreInteractions(root);
    }

    @Test
    void onEachClassPrivateDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(
                mock(RootScope.class),
                PrivateDirectiveProcessor.class));
    }

    @Test
    void onEachClassNoNullaryDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(
                mock(RootScope.class),
                NoNullaryDirectiveProcessor.class));
    }

    @Test
    void onEachClass() {
        RootScope root = new RootScope("", mock(FileBackend.class), null);

        new DirectivePlugin().onEachClass(root, TestDirectiveProcessor.class);

        assertThat(root.getSettings(DirectiveSettings.class).getProcessors())
            .hasEntrySatisfying("test", v -> assertThat(v).isInstanceOf(TestDirectiveProcessor.class));
    }

}