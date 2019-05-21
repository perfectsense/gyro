package gyro.core.repo;

import com.psddev.test.AbstractBeanTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RepositorySettingsTest extends AbstractBeanTest<RepositorySettings> {

    @Test
    void centralId() {
        assertThat(RepositorySettings.CENTRAL.getId()).isEqualTo("central");
    }

    @Test
    void centralUrl() {
        assertThat(RepositorySettings.CENTRAL.getUrl()).isEqualTo("https://repo.maven.apache.org/maven2");
    }

    @Test
    void getRepositoriesNew() {
        assertThat(new RepositorySettings().getRepositories()).containsExactly(RepositorySettings.CENTRAL);
    }

}