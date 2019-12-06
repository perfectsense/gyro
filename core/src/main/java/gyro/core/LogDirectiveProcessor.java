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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import gyro.util.Bug;

@Type("log")
public class LogDirectiveProcessor extends PrintDirectiveProcessor {

    @Override
    protected void print(String content) {
        try {
            Path home = GyroCore.getHomeDirectory();
            Files.createDirectories(home);

            Path log = home.resolve("debug.log");
            try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
                printWriter.write(content);
                printWriter.write("\n");

            }
        } catch (IOException error) {
            throw new Bug(error);
        }
    }

}
