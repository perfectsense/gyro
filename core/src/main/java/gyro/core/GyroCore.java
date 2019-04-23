package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.ThreadLocalStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static Path getRootInitFile() {
        Path path = Paths.get("").toAbsolutePath();
        while (path != null) {
            if (Files.isDirectory(path)) {
                Path initFile = path.resolve(Paths.get(".gyro", "init.gyro"));
                if (Files.exists(initFile) && Files.isRegularFile(initFile)) {
                    return initFile;
                }
            }

            path = path.getParent();
        }

        throw new InitFileNotFoundException("Not a gyro project directory, use 'gyro init <plugins>...' to create one. "
             + "See 'gyro help init' for detailed usage.");
    }
}
