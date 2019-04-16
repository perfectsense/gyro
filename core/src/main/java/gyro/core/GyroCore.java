package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.ThreadLocalStack;

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
}
