package gyro.core;

import com.psddev.dari.util.ThreadLocalStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class BeamCore {

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

    public static boolean verifyConfig(Path path) throws IOException {
        Path rootPath = findPluginPath().getParent();
        Path configRootPath = findRootDirectory(Paths.get(path.toFile().getCanonicalPath()));

        if (configRootPath == null || !Files.isSameFile(rootPath, configRootPath)) {
            throw new BeamException(String.format("'%s' is not located within the current working directory '%s'!", path, rootPath.getParent()));
        }

        return true;
    }

    public static Path findPluginPath() throws IOException {
        Path rootPath = findRootDirectory(Paths.get("").toAbsolutePath());
        String message = "Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.";
        if (rootPath == null) {
            throw new BeamException(message);
        }

        Path pluginPath = rootPath.resolve(Paths.get("plugins.gyro"));
        if (!pluginPath.toFile().exists()) {
            throw new BeamException(message);
        }

        return pluginPath;
    }

    public static Path findCommandPluginPath() throws IOException {
        Path rootPath = findRootDirectory(Paths.get("").toAbsolutePath());
        if (rootPath != null) {
            return rootPath.resolve(Paths.get("plugins.gyro"));
        } else {
            return null;
        }
    }

    private static Path findRootDirectory(Path path) throws IOException {
        while (path != null) {
            if (path.toFile().isDirectory()) {
                try (Stream<Path> stream = Files.list(path)) {
                    Optional<Path> optional = stream.filter(t -> t.toFile().isDirectory()
                        && t.getFileName() != null
                        && t.getFileName().toString().equals(".gyro")).findAny();

                    if (optional.isPresent()) {
                        return optional.get().normalize();
                    }
                }
            }

            path = path.getParent();
        }

        return null;
    }
}
