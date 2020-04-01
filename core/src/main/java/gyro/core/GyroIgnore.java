package gyro.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;

public class GyroIgnore {

    public static final String IGNORE_FILE = ".gyro/.gyroignore";
    private static List<String> ignorePatterns = null;

    /**
     * The primary method to determine if a file should be excluded from gyro processes
     * The pattern from the gyroignore file are only compiled and processed once and reused thereafter
     * The raw patterns are processed to handle special cases
     *
     * @param rootDirectory The root directory of the project, used to determine the gyroignore file path
     * @param filePath The file path being tested
     * @return True if file is to be ignored, False otherwise
     */
    public static boolean ignoreFile(Path rootDirectory, String filePath) {
        // Only set the ignorePattern list the first time
        // Reuse the pattern there after
        if (ignorePatterns == null) {
            Path gyroIgnorePath = rootDirectory.resolve(rootDirectory.toString().replace("/.gyro/state", "/"))
                .resolve(IGNORE_FILE);
            try {
                ignorePatterns = Files.exists(gyroIgnorePath) ? Files.lines(gyroIgnorePath)
                    .filter(o -> !o.startsWith("#"))
                    .filter(o -> !ObjectUtils.isBlank(o))
                    .map(GyroIgnore::processedPattern)
                    .collect(Collectors.toList()) : Collections.emptyList();
            } catch (IOException ex) {
                ignorePatterns = Collections.emptyList();
            }
        }

        // A set of file path to be included even though patterns dictate it should be excluded
        // patterns starting with a `!` indicate exceptional cases
        Set<String> filePathExceptionList = new HashSet<>();

        return ignorePatterns.stream()
            .anyMatch(pattern -> fileMatchesIgnorePattern(
                pattern,
                filePath,
                filePathExceptionList,
                rootDirectory.endsWith(".gyro/state")));
    }

    private static String processedPattern(String pattern) {
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

    /**
     * To check if a file path matches the supplied pattern and is not part of the exception list
     * @param pattern The pattern to match the file path with
     * @param filePath The file path to be determined if it is a match
     * @param filePathExceptionList A list of file path that is excluded from the matching
     * @param isStateFile A flag to know if the file path resembles a state file
     * @return True if the file path matches teh pattern, False otherwise
     */
    private static boolean fileMatchesIgnorePattern(
        String pattern,
        String filePath,
        Set<String> filePathExceptionList,
        boolean isStateFile) {

        // Remove the portion of the ptah that resembles it is a state file
        // The patterns mentioned in the gyroignore file dont take into state file paths
        // If the configured gyro file is to be ignore then so will its state file
        filePath = isStateFile ? filePath.replace("/.gyro/state", "/") : filePath;

        try {
            // For patterns starting with a `!` character resembles a special case
            // If the pattern matches the file path, then the file needs to be included
            if (pattern.startsWith("!")) {
                pattern = pattern.replaceFirst("!", "");
                if (Pattern.matches(pattern, filePath)) {
                    filePathExceptionList.add(filePath);
                }

                return false;
            } else {
                return Pattern.matches(pattern, filePath) && !filePathExceptionList.contains(filePath);
            }
        } catch (PatternSyntaxException ex) {
            //ignore a bad pattern
            return false;
        }
    }
}
