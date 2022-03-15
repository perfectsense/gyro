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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import gyro.core.scope.Settings;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;

public class RepositorySettings extends Settings {

    private List<RemoteRepository> repositories;

    public static final RemoteRepository CENTRAL;

    public static final RemoteRepository RELEASE;

    static {
        URL proxyUrl = RepositoryDirectiveProcessor.proxy();
        Proxy proxy = null;
        if (proxyUrl != null) {
            proxy = new Proxy(proxyUrl.getProtocol(), proxyUrl.getHost(), proxyUrl.getPort());
        }

        CENTRAL = new RemoteRepository
            .Builder("central", "default", "https://repo.maven.apache.org/maven2")
            .setProxy(proxy)
            .build();

        RELEASE = new RemoteRepository.Builder(
            "https://artifactory.psdops.com/gyro-releases",
            "default",
            "https://artifactory.psdops.com/gyro-releases")
            .setProxy(proxy)
            .build();
    }

    public List<RemoteRepository> getRepositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();

            repositories.add(RELEASE);
            repositories.add(CENTRAL);
        }

        return repositories;
    }

    public void setRepositories(List<RemoteRepository> repositories) {
        this.repositories = repositories;
    }

}
