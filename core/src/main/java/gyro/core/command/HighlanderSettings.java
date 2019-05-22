package gyro.core.command;

import gyro.core.resource.Settings;

public class HighlanderSettings extends Settings {

    private boolean highlander;

    public boolean isHighlander() {
        return highlander;
    }

    public void setHighlander(boolean highlander) {
        this.highlander = highlander;
    }

}
