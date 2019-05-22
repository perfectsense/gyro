package gyro.core.plugin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PluginClassLoaderTest {

    @Test
    void constructor() {
        PluginClassLoader loader = new PluginClassLoader();

        assertThat(loader.getURLs()).hasSize(0);
        assertThat(loader.getParent()).isEqualTo(PluginClassLoader.class.getClassLoader());
    }

}