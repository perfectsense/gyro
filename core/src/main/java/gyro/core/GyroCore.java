package gyro.core;

import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.ThreadLocalStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GyroCore {

    public static final String INIT_FILE = ".gyro/init.gyro";

    private static final ThreadLocalStack<GyroUI> UI = new ThreadLocalStack<>();

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

    public static Path getRootDirectory() {
        return ROOT_DIRECTORY.get();
    }
}
