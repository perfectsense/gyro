package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.ThreadLocalStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class GyroCore {

    private static final ThreadLocalStack<GyroUI> UI = new ThreadLocalStack<>();

    public static GyroUI ui() {
        return UI.get();
    }

    public static void pushUi(GyroUI ui) {
        UI.push(ui);
    }

    public static GyroUI popUi() {
        return UI.pop();
    }

    public static String getGyroUserHome() {
        String userHome = System.getenv("GYRO_USER_HOME");
        if (ObjectUtils.isBlank(userHome)) {
            userHome = System.getProperty("user.home");
        }

        return userHome;
    }

    public static Path getRootInitFile() throws IOException {
        Path rootDir = findRootDirectory(Paths.get("").toAbsolutePath());
        if (rootDir != null) {
            Path initFile = rootDir.resolve(Paths.get("init.gyro"));
            if (Files.exists(initFile) && Files.isRegularFile(initFile)) {
                return initFile;
            }
        }

        throw new InitFileNotFoundException("Not a gyro project directory, use 'gyro init <plugins>...' to create one. "
             + "See 'gyro help init' for detailed usage.");
    }

    private static Path findRootDirectory(Path path) throws IOException {
        while (path != null) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.list(path)) {
                    Optional<Path> optional = stream.filter(t -> Files.isDirectory(t)
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
