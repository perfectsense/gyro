package gyro.core.plugin;

import com.google.common.collect.ImmutableSet;
import com.psddev.test.AbstractBeanTest;
import com.psddev.test.BeanProperty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@BeanProperty(name = "call", getter = false)
class PluginSettingsTest extends AbstractBeanTest<PluginSettings> {

    @Test
    void addClasses() throws Exception {
        PluginSettings settings = new PluginSettings();
        Class<?> testClass = getClass();

        settings.addClasses(ImmutableSet.of(TestPlugin.class, testClass));

        assertThat(settings.getPlugins())
            .hasSize(1)
            .allSatisfy(plugin -> assertThat(plugin).isInstanceOf(TestPlugin.class));

        assertThat(settings.getOtherClasses()).containsExactly(testClass);

        TestPlugin plugin = (TestPlugin) settings.getPlugins().get(0);

        assertThat(plugin.counts).hasSize(1);
        assertThat(plugin.counts.get(testClass)).isEqualTo(1);
    }

    @Test
    void addClassesTwice() throws Exception {
        PluginSettings settings = new PluginSettings();
        Class<?> testClass = getClass();

        settings.addClasses(ImmutableSet.of(testClass));

        assertThat(settings.getPlugins()).isEmpty();
        assertThat(settings.getOtherClasses()).containsExactly(testClass);

        settings.addClasses(ImmutableSet.of(TestPlugin.class));

        TestPlugin plugin = (TestPlugin) settings.getPlugins().get(0);

        assertThat(plugin.counts).hasSize(1);
        assertThat(plugin.counts.get(testClass)).isEqualTo(1);
    }

    @Test
    void addClassesException() {
        PluginSettings settings = new PluginSettings();

        assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> settings.addClasses(ImmutableSet.of(TestExceptionPlugin.class, getClass())));
    }

}