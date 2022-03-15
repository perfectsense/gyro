/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        assertThat(new RepositorySettings().getRepositories()).containsExactly(
            RepositorySettings.RELEASE,
            RepositorySettings.CENTRAL);
    }

}
