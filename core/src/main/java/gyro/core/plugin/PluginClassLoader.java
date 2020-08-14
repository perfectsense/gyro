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

package gyro.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

class PluginClassLoader extends URLClassLoader {

    private List<URL> pluginUrls;

    public PluginClassLoader() {
        super(new URL[0], PluginClassLoader.class.getClassLoader());
    }

    public void add(List<URL> urls) {
        urls.forEach(this::addURL);
        pluginUrls = urls;
    }

    public List<URL> getPluginUrls() {
        return pluginUrls;
    }
}
