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

import java.net.MalformedURLException;
import java.net.URL;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;

@Type("repository")
public class RepositoryDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        String url = getArgument(scope, node, String.class, 0);

        URL proxyUrl = proxy();
        Proxy proxy = null;
        if (proxyUrl != null) {
            proxy = new Proxy(proxyUrl.getProtocol(), proxyUrl.getHost(), proxyUrl.getPort());
        }

        RemoteRepository remoteRepository = new RemoteRepository.Builder(url, "default", url)
            .setProxy(proxy)
            .build();

        scope.getSettings(RepositorySettings.class)
            .getRepositories()
            .add(0, remoteRepository);
    }

    static URL proxy() {
        String proxy = System.getenv("http_proxy") != null
            ? System.getenv("http_proxy")
            : System.getenv("https_proxy");

        if (proxy != null) {
            try {
                return new URL(proxy);
            } catch (MalformedURLException ex) {
                throw new GyroException(ex);
            }
        }

        return null;
    }
}
