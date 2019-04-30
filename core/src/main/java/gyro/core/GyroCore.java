package gyro.core;

import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ThreadLocalStack;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GyroCore {

    public static final String INIT_FILE = ".gyro/init.gyro";

    private static final ThreadLocalStack<GyroUI> UI = new ThreadLocalStack<>();

    private static final Lazy<Path> HOME_DIRECTORY = new Lazy<Path>() {

        @Override
        protected Path create() {
            String homeDir = System.getenv("GYRO_USER_HOME");

            return Paths.get(StringUtils.isNotBlank(homeDir)
                ? homeDir
                : System.getProperty("user.home"));
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

            throw new InitFileNotFoundException("Not a gyro project directory, use 'gyro init <plugins>...' to create one. "
                + "See 'gyro help init' for detailed usage.");
        }
    };

    private static final Lazy<FileBackend> ROOT_DIRECTORY_BACKEND = new Lazy<FileBackend>() {

        @Override
        protected FileBackend create() {
            return new LocalFileBackend(ROOT_DIRECTORY.get());
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

    public static Path getHomeDirectory() {
        return HOME_DIRECTORY.get();
    }

    public static Path getRootDirectory() {
        return ROOT_DIRECTORY.get();
    }

    public static FileBackend getRootDirectoryBackend() {
        return ROOT_DIRECTORY_BACKEND.get();
    }

}
