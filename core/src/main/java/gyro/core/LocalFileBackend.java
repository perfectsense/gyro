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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import com.psddev.dari.util.IoUtils;

public class LocalFileBackend extends FileBackend {

    private final Path rootDirectory;

    public LocalFileBackend(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Stream<String> list() throws IOException {
        if (Files.exists(rootDirectory)) {
            return Files.find(rootDirectory, Integer.MAX_VALUE, (file, attributes) -> attributes.isRegularFile())
                .map(rootDirectory::relativize)
                .map(Path::toString)
                .filter(f -> !f.startsWith(".gyro" + File.separator) && f.endsWith(".gyro"));

        } else {
            return Stream.empty();
        }
    }

    @Override
    public InputStream openInput(String file) throws IOException {
        return Files.newInputStream(rootDirectory.resolve(file).normalize());
    }

    @Override
    public OutputStream openOutput(String file) throws IOException {
        Path finalFile = rootDirectory.resolve(file);
        Path finalDir = finalFile.getParent();

        Files.createDirectories(finalDir);

        Path tempFile = Files.createTempFile(finalDir, ".local-file-backend-", ".gyro.tmp");

        tempFile.toFile().deleteOnExit();

        return new FileOutputStream(tempFile.toString()) {

            @Override
            public void close() throws IOException {
                super.close();
                Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }

    @Override
    public void delete(String file) throws IOException {
        Files.deleteIfExists(rootDirectory.resolve(file));
    }

    @Override
    public boolean exists(String file) throws Exception {
        return Files.exists(rootDirectory.resolve(file));
    }

    @Override
    public void copy(String source, String destination) throws Exception {
        try (InputStream inputStream = openInput(source); OutputStream outputStream = openOutput(destination)) {
            IoUtils.copy(inputStream, outputStream);
        }
    }

    @Override
    public String toString() {
        return rootDirectory.toString();
    }

    public boolean deleteDirectory() {
        return deleteDirectory(rootDirectory.toFile());
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }

        return directoryToBeDeleted.delete();
    }
}
