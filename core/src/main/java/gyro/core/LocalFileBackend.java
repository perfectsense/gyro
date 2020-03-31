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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.util.ObjectUtils;

public class LocalFileBackend extends FileBackend {

    private final Path rootDirectory;

    public LocalFileBackend(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Stream<String> list() throws IOException {
        if (Files.exists(rootDirectory)) {
            Path rootPath = rootDirectory.resolve(rootDirectory.toString().replace("/.gyro/state", "/"));
            List<String> ignoreList = Files.lines(rootPath.resolve(GyroCore.IGNORE_FILE))
                .filter(o -> !o.startsWith("#"))
                .filter(o -> !ObjectUtils.isBlank(o))
                .collect(Collectors.toList());

            return Files.find(rootDirectory, Integer.MAX_VALUE, (file, attributes) -> attributes.isRegularFile())
                .map(rootDirectory::relativize)
                .map(Path::toString)
                .filter(f -> !f.startsWith(".gyro" + File.separator) && f.endsWith(".gyro"))
                .filter(f -> !ignoreFile(ignoreList, f));
        } else {
            return Stream.empty();
        }
    }

    private boolean ignoreFile(List<String> ignoreList, String filePath) {
        Set<String> specialCase = new HashSet<>();

        return ignoreList.stream()
            .anyMatch(pattern -> ignoreFile(pattern, filePath, specialCase, rootDirectory.endsWith(".gyro/state")));
    }

    private boolean ignoreFile(String pattern, String filePath, Set<String> specialCase, boolean isStateFile) {
        String processedPattern = processedPattern(pattern);
        filePath = isStateFile ? filePath.replace("/.gyro/state", "/") : filePath;

        try {
            if (processedPattern.startsWith("!")) {
                processedPattern = processedPattern.replaceFirst("!", "");
                if (Pattern.matches(processedPattern, filePath)) {
                    specialCase.add(filePath);
                }

                return false;
            } else {
                System.out.println(String.format(
                    "\n -->*** pattern - [%s] - transformed - [%s] - string - [%s] - flag - [%s]",
                    pattern,
                    processedPattern,
                    filePath,
                    Pattern.matches(processedPattern, filePath) && !specialCase.contains(filePath)));
                return Pattern.matches(processedPattern, filePath) && !specialCase.contains(filePath);
            }
        } catch (PatternSyntaxException ex) {
            //ignore
            return false;
        }
    }

    private String processedPattern(String pattern) {
        pattern = pattern.replaceAll("\\.", "\\.");

        if (pattern.matches(".*/(\\*)\\1+")) { // pattern --> /**, /***, /**** => /**
            pattern = pattern.replaceAll("(\\*)\\1+$", "**");
        } else if (pattern.matches(".*[^/](\\*)\\1+")) { // pattern --> a**, a***, a/b**, a/b*** => a*, a*, a/b*, a/b*
            pattern = pattern.replaceAll("(\\*)\\1+$", "*");
        }

        if (pattern.endsWith("*") && !pattern.endsWith("/**")) {
            pattern = pattern.substring(0, pattern.lastIndexOf("*")) + "[^/]*";
        }

        if (pattern.matches("(\\*)\\1+/")) {
            // anything post **/
            pattern = pattern.replaceFirst("(\\*)\\1+/", "*/");
        }

        if (pattern.endsWith("/**")) {
            // anything under the pattern before /**
            pattern = pattern.substring(0, pattern.lastIndexOf("/**")) + "/*";
        }

        if (pattern.contains("/**/")) {
            // anything before and after /**/
            pattern = pattern.replaceAll("/\\*\\*/", "/*/");
        }

        if (pattern.contains(".")) {
            pattern = pattern.replaceAll("\\.", "\\\\.");
        }

        if (pattern.contains("**")) { // replace redundant * with single *
            pattern = pattern.replaceAll("(\\*)\\1+", "*");
        }

        if (pattern.contains("*")) { // escape * so that it is a valid regex
            pattern = pattern.replaceAll("\\*", ".*");
        }

        if (pattern.contains("].*")) { // revert an exception case of escaping *
            pattern = pattern.replaceAll("].\\*", "]*");
        }

        if (!pattern.endsWith("*")) { // add regex end symbol
            pattern = pattern + "$";
        }

        return pattern;
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
    public String toString() {
        return rootDirectory.toString();
    }

}
