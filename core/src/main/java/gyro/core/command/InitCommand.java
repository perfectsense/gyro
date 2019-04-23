package gyro.core.command;

import com.google.common.base.Charsets;
import com.psddev.dari.util.IoUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Command(name = "init", description = "Initialize a Gyro working directory.")
public class InitCommand extends AbstractCommand {

    @Arguments(description = "A list of plugins specified in the format of <group>:<artifact>:<version>. "
        + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT")
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
                throw new GyroException("Plugins have to be specified in the format of <group>:<artifact>:<version>. "
                     + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT");
            }

            String group = parts[0];
            String artifact = parts[1];
            String version = parts[2];

            String template = IoUtils.toString(getClass().getResourceAsStream("/plugin.gyro"), Charsets.UTF_8);

            template = template.replace("${GROUP}", group);
            template = template.replace("${ARTIFACT}", artifact);
            template = template.replace("${VERSION}", version);
            pluginBuilder.append(template);
        }

        Path rootDir = Paths.get(".gyro");
        if (!Files.exists(rootDir)) {
            Files.createDirectories(rootDir);
            try (PrintWriter printWriter = new PrintWriter(
                Files.newBufferedWriter(Paths.get(rootDir.toString(), "init.gyro"), StandardCharsets.UTF_8))) {

                printWriter.write(pluginBuilder.toString());
            }

            GyroCore.ui().write("New Gyro working directory has been created.\n");

        } else if (Files.isDirectory(rootDir)) {
            if (GyroCore.ui().readBoolean(
                    Boolean.FALSE,
                    "\nFound existing Gyro working directory at '%s', are you sure you want to update plugins?",
                    rootDir.normalize())) {

                try (PrintWriter printWriter = new PrintWriter(
                    Files.newBufferedWriter(Paths.get(rootDir.toString(), "init.gyro"), StandardCharsets.UTF_8))) {

                    printWriter.write(pluginBuilder.toString());
                }

                GyroCore.ui().write("Gyro working directory has been updated.\n");
            }

        } else {
            throw new GyroException(String.format("Unable to update Gyro working directory, file already exist at '%s'", rootDir.normalize()));
        }
    }
}
