package gyro.core.diff;

import gyro.core.GyroUI;

public enum ExecutionResult {

    OK {
        public void write(GyroUI ui) {
            ui.write(ui.isVerbose() ? "\n@|bold,green OK|@\n\n" : " @|bold,green OK|@\n");
        }
    },

    SKIPPED {
        public void write(GyroUI ui) {
            ui.write(ui.isVerbose() ? "\n@|bold,yellow SKIPPED|@\n\n" : " @|bold,yellow SKIPPED|@\n");
        }
    };

    public abstract void write(GyroUI ui);

}
