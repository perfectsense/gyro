package gyro.core.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.psddev.dari.util.IoUtils;
import gyro.core.GyroCore;
import io.airlift.airline.Command;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Command(name = "version", description = "Display the version of Gyro.")
public class VersionCommand extends AbstractCommand {

    @Override
    protected void doExecute() {
        VersionCommand.printVersion();
        VersionCommand.printUpdateVersion();
    }

    public static void printVersion() {
        ComparableVersion currentVersion = VersionCommand.getCurrentVersion();
        VersionCommand.renderVersionMessage(currentVersion.toString());
    }

    public static void printUpdateVersion() {
        ComparableVersion currentVersion = VersionCommand.getCurrentVersion();
        ComparableVersion latestVersion = VersionCommand.getLatestVersion();

        if (latestVersion != null) {
            Boolean isUpdateAvailable = currentVersion.compareTo(latestVersion) < 0;
            if (isUpdateAvailable) {
                VersionCommand.renderUpdateMessage(latestVersion.toString(), getOsName());
            }
        }
    }

    public static ComparableVersion getCurrentVersion() {
        ComparableVersion currentVersion = null;
        InputStream stream = VersionCommand.class.getResourceAsStream("/gyro.properties");
        Properties properties = new Properties();
        try {
            properties.load(stream);
            currentVersion = new ComparableVersion((String) properties.get("version"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return currentVersion;
    }

    public static ComparableVersion getLatestVersion() {
        ComparableVersion latestVersion = null;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            URL artifactoryMetadataUrl = new URL(
                "https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-" + getOsName()
                    + "/maven-metadata.xml");
            String artifactoryMetadata = IoUtils.toString(artifactoryMetadataUrl);

            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document dDoc = builder.parse(new InputSource(new StringReader(artifactoryMetadata)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            String version = (String) xpath.evaluate("/metadata/versioning/latest", dDoc, XPathConstants.STRING);

            latestVersion = new ComparableVersion((String) version);
        } catch (
            Exception error) {
            GyroCore.ui().write("@|red Error when trying to get the latest version info.|@\n");
        }

        return latestVersion;
    }

    private static String getOsName() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("mac")) {
            name = "osx";
        } else if (name.contains("win")) {
            name = "windows";
        } else if (name.contains("nux")) {
            name = "linux";
        } else {
            name = "osx";
        }

        return name;
    }

    private static void renderVersionMessage(String version) {
        GyroCore.ui().write("\nYou're running version @|blue %s|@ of Gyro.\n", version);
    }

    private static void renderUpdateMessage(String latestVersion, String osName) {
        GyroCore.ui().write("A new version of Gyro is available: @|blue %s|@\n", latestVersion);
        GyroCore.ui()
            .write(
                "Download the new release at @|blue https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-%1$s/%2$s/gyro-cli-%1$s-%2$s.zip|@\n",
                osName,
                latestVersion);
    }
}
