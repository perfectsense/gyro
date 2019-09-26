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

import java.util.ArrayList;
import java.util.List;

import gyro.core.scope.Settings;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositorySettings extends Settings {

    /**
     * The default Maven central repository.
     *
     * @see <a href="https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Super_POM">Super POM</a>
     */
    public static final RemoteRepository CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

    private List<RemoteRepository> repositories;

    public List<RemoteRepository> getRepositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();

            repositories.add(CENTRAL);
        }

        return repositories;
    }

    public void setRepositories(List<RemoteRepository> repositories) {
        this.repositories = repositories;
    }

}
