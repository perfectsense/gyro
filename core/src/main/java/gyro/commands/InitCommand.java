package gyro.commands;

import com.google.common.base.Charsets;
import com.psddev.dari.util.IoUtils;
import gyro.core.BeamCore;
import gyro.core.BeamException;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

@Command(name = "init", description = "Initialize a Gyro working directory.")
public class InitCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    public List<String> arguments() {
        return arguments;
    }

    @Override
    protected void doExecute() throws Exception {
        StringBuilder pluginBuilder = new StringBuilder();
        for (String plugin : arguments()) {
            if (!plugin.contains(":")) {
                throw new BeamException(String.format("Plugins have to be specified with a version, i.e. %s:[version]", plugin));
            }

            String name = plugin.split(":")[0];
            String version = plugin.split(":")[1];

            String template = IoUtils.toString(getClass().getResourceAsStream("/plugin.gyro"), Charsets.UTF_8);

            template = template.replaceAll("\\$\\{NAME}", name);
            template = template.replaceAll("\\$\\{VERSION}", version);
            pluginBuilder.append(template);
            pluginBuilder.append("\n");
        }

        File rootDir = new File(".gyro");
        if (rootDir.exists()) {
            throw new BeamException(String.format("Gyro config directory already exist at '%s'", rootDir.getCanonicalFile()));
        }

        rootDir.mkdirs();
        File pluginsFile = new File(rootDir, "plugins.gyro");
        try (FileWriter writer = new FileWriter(pluginsFile)) {
            writer.write(pluginBuilder.toString());
        }

        BeamCore.ui().write("New Gyro working directory has been created.\n");
    }
}
