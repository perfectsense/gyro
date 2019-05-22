package gyro.core.directive;

import gyro.core.FileBackend;
import gyro.core.resource.RootScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectivePluginTest {

    @Test
    void onEachClassNotDirectiveProcessor() throws IllegalAccessException, InstantiationException {
        RootScope root = mock(RootScope.class);

        new DirectivePlugin().onEachClass(root, getClass());

        verifyNoMoreInteractions(root);
    }

    @Test
    void onEachClassPrivateDirectiveProcessor() {
        assertThatExceptionOfType(IllegalAccessException.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(
                mock(RootScope.class),
                PrivateDirectiveProcessor.class));
    }

    @Test
    void onEachClassNoNullaryDirectiveProcessor() {
        assertThatExceptionOfType(InstantiationException.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(
                mock(RootScope.class),
                NoNullaryDirectiveProcessor.class));
    }

    @Test
    void onEachClass() throws IllegalAccessException, InstantiationException {
        RootScope root = new RootScope("", mock(FileBackend.class), null, null);

        new DirectivePlugin().onEachClass(root, TestDirectiveProcessor.class);

        assertThat(root.getSettings(DirectiveSettings.class).getProcessors())
            .hasEntrySatisfying("test", v -> assertThat(v).isInstanceOf(TestDirectiveProcessor.class));
    }

}