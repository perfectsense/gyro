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
        Path configRootPath = findWorkingDirectory(path);

        if (configRootPath == null || !Files.isSameFile(rootPath, configRootPath)) {
            throw new BeamException(String.format("'%s' is not located within the current working directory '%s'!", path, rootPath.getParent()));
        }

        return true;
    }

    public static Path findPluginPath() throws IOException {
        Path rootPath = findWorkingDirectory(Paths.get("").toAbsolutePath());
        if (rootPath == null) {
            throw new BeamException("Unable to find gyro working directory, use `gyro init [<plugin>:<version>...]` to create one.");
        }

        Path pluginPath = rootPath.resolve(Paths.get("plugins.gyro"));
        if (!pluginPath.toFile().exists()) {
            throw new BeamException(String.format(
                "Unable to find 'plugins.gyro', use `gyro init [<plugin>:<version>...]` under '%s' directory to create it.", rootPath.getParent()));
        }

        return pluginPath;
    }

    public static Path findEnterpriseConfigPath() throws IOException {
        return  findPluginPath().getParent().resolve(Paths.get("enterprise.gyro"));
    }

    private static Path findWorkingDirectory(Path path) throws IOException {
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
