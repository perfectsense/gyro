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

    private static Path rootDir = null;

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

    public static Path findPluginConfigPath(Path path) throws IOException {
        return findWorkingDirectory(path).resolve(Paths.get("plugins.gyro"));
    }

    public static Path findEnterpriseConfigPath() {
        return rootDir != null ? rootDir.resolve(Paths.get("enterprise.gyro")) : null;
    }

    private static Path findWorkingDirectory(Path path) throws IOException {
        if (rootDir != null) {
            return rootDir;
        }

        while (path != null) {
            if (path.toFile().isDirectory()) {
                try (Stream<Path> stream = Files.list(path)) {
                    Optional<Path> optional = stream.filter(t -> t.toFile().isDirectory()
                        && t.getFileName() != null
                        && t.getFileName().toString().equals(".gyro")).findAny();

                    if (optional.isPresent()) {
                        rootDir = optional.get().normalize();
                        return rootDir;
                    }
                }
            }

            path = path.getParent();
        }

        throw new BeamException("Unable to find working directory, use `gyro init [<plugin>:<version>...]` to create one.");
    }
}
