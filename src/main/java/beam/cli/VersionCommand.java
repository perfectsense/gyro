package beam.cli;

import io.airlift.command.Command;

import java.io.InputStream;
import java.util.Properties;

@Command(name = "version", description = "Beam version.")
public class VersionCommand implements Runnable {

    @Override
    public void run() {
        InputStream stream = VersionCommand.class.getResourceAsStream("/build.properties");
        Properties properties = new Properties();

        try {
            properties.load(stream);
            System.out.println("Beam version " + properties.get("version"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}