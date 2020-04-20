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

package gyro.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ThreadLocalStack;
import org.apache.commons.lang3.StringUtils;

public class GyroCore {

    public static final String INIT_FILE = ".gyro/init.gyro";

    private static final ThreadLocalStack<GyroUI> UI = new ThreadLocalStack<>();

    private static final ConcurrentMap<String, FileBackend> STATE_BACKENDS = new ConcurrentHashMap<>();

    private static final Lazy<Path> HOME_DIRECTORY = new Lazy<Path>() {

        @Override
        protected Path create() {
            String homeDir = System.getenv("GYRO_USER_HOME");

            return Paths.get(
                StringUtils.isNotBlank(homeDir) ? homeDir : System.getProperty("user.home"),
                ".gyro");
        }
    };

    private static final Lazy<Path> ROOT_DIRECTORY = new Lazy<Path>() {

        @Override
        protected Path create() {
            for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
                Path initFile = dir.resolve(INIT_FILE);

                if (Files.exists(initFile) && Files.isRegularFile(initFile)) {
                    return dir;
                }
            }

            return null;
        }
    };

    public static GyroUI ui() {
        return UI.get();
    }

    public static void pushUi(GyroUI ui) {
        UI.push(ui);
    }

    public static GyroUI popUi() {
        return UI.pop();
    }

    public static void putStateBackends(Map<String, FileBackend> stateBackends) {
        stateBackends.forEach(STATE_BACKENDS::put);
    }

    public static FileBackend getStateBackend(String key) {
        return STATE_BACKENDS.get(key);
    }

    public static Path getHomeDirectory() {
        return HOME_DIRECTORY.get();
    }

    public static Path getRootDirectory() {
        return ROOT_DIRECTORY.get();
    }

}
