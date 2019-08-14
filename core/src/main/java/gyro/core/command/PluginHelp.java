package gyro.core.command;

import io.airlift.airline.Help;

import java.util.Collections;

public class PluginHelp extends Help {

    @Override
    public void run() {
        help(global, Collections.singletonList("plugin"));
    }

}
