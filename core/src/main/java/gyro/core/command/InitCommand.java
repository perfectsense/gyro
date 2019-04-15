package gyro.core.command;

import com.google.common.base.Charsets;
import com.psddev.dari.util.IoUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

@Command(name = "init", description = "Initialize a Gyro working directory.")
public class InitCommand extends AbstractCommand {

    @Arguments(description = "A list of plugins specified in the format of <group>:<artifact>:<version>. For example: gyro:gyro-aws-provider:0.1-SNAPSHOT")
    private List<String> plugins;

    public List<String> plugins() {
        if (plugins == null) {
            plugins = new ArrayList<>();
        }

        return plugins;
    }

    @Override
    protected void doExecute() throws Exception {
        StringBuilder pluginBuilder = new StringBuilder();
        for (String plugin : plugins()) {
            String [] parts = plugin.split(":");
            if (parts.length != 3) {
                throw new GyroException("Plugins have to be specified in the format of <group>:<artifact>:<version>. For example: gyro:gyro-aws-provider:0.1-SNAPSHOT");
            }

            String group = plugin.split(":")[0];
            String artifact = plugin.split(":")[1];
            String version = plugin.split(":")[2];

            String template = IoUtils.toString(getClass().getResourceAsStream("/plugin.gyro"), Charsets.UTF_8);

            template = template.replaceAll("\\$\\{GROUP}", group);
            template = template.replaceAll("\\$\\{ARTIFACT}", artifact);
            template = template.replaceAll("\\$\\{VERSION}", version);
            pluginBuilder.append(template);
            pluginBuilder.append("\n");
        }

        File rootDir = new File(".gyro");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
            File pluginsFile = new File(rootDir, "plugins.gyro");
            try (FileWriter writer = new FileWriter(pluginsFile)) {
                writer.write(pluginBuilder.toString());
            }

            GyroCore.ui().write("New Gyro working directory has been created.\n");

        } else if (rootDir.isDirectory()) {
            if (GyroCore.ui().readBoolean(
                    Boolean.FALSE,
                    "\nFound existing Gyro working directory at '%s', are you sure you want to update plugins?",
                    rootDir.getCanonicalPath())) {

                File pluginsFile = new File(rootDir, "plugins.gyro");
                try (FileWriter writer = new FileWriter(pluginsFile)) {
                    writer.write(pluginBuilder.toString());
                }

                GyroCore.ui().write("Gyro working directory has been updated.\n");
            }

        } else {
            throw new GyroException(String.format("Unable to update Gyro working directory, file already exist at '%s'", rootDir.getCanonicalPath()));
        }
    }
}
