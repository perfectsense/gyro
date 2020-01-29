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

package gyro.core.backend;

import gyro.core.FileBackend;
import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class FileBackendPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (FileBackend.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends FileBackend> fileBackendClass = (Class<? extends FileBackend>) aClass;
            String namespace = Reflections.getNamespace(fileBackendClass);
            String type = Reflections.getType(fileBackendClass);

            root.getSettings(FileBackendsSettings.class)
                .getFileBackendsClasses()
                .put(namespace + "::" + type, fileBackendClass);
        }
    }

}
