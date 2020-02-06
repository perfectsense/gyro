package gyro.core.command;

import java.io.FileNotFoundException;
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
import gyro.core.GyroException;
import io.airlift.airline.Command;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Command(name = "version", description = "Display the version of Gyro.")
public class VersionCommand extends AbstractCommand {

    @Override
    protected void doExecute() throws Exception {
        VersionCommand.printVersion();
        VersionCommand.printUpdateVersion();
    }

    public static void printVersion() throws IOException {
        InputStream stream = VersionCommand.class.getResourceAsStream("/gyro.properties");
        Properties properties = new Properties();
        try {
            properties.load(stream);
            ComparableVersion currentVersion = VersionCommand.getCurrentVersion(properties);
            VersionCommand.renderVersion(currentVersion.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printUpdateVersion() throws IOException {
        InputStream stream = VersionCommand.class.getResourceAsStream("/gyro.properties");
        Properties properties = new Properties();
        try {
            properties.load(stream);
            ComparableVersion currentVersion = VersionCommand.getCurrentVersion(properties);
            ComparableVersion latestVersion = VersionCommand.getLatestVersion(properties);

            Boolean isUpdateAvailable = currentVersion.compareTo(latestVersion) < 0;
            if (isUpdateAvailable) {
                VersionCommand.renderUpdateMessage(latestVersion.toString(), properties.get("osName").toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void renderVersion(String version) {
        GyroCore.ui().write("@|white Gyro version:\t%s\n|@", version);
    }

    private static void renderUpdateMessage(String latestVersion, String osName) {
        GyroCore.ui().write("@|green,bold Latest version:\t%s\n|@", latestVersion);
        GyroCore.ui()
            .write(
                "@|green Update here:|@\t%s\n",
                "https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-osx/" + latestVersion
                    + "/gyro-cli-" + osName + "-"
                    + latestVersion + ".zip", osName);
    }

    public static ComparableVersion getCurrentVersion(Properties properties) {
        return new ComparableVersion((String) properties.get("version"));
    }

    public static ComparableVersion getLatestVersion(Properties properties) throws FileNotFoundException {
        ComparableVersion latestVersion;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            URL artifactoryMetadataUrl = new URL(
                "https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-" + properties.get("osName")
                    + "/maven-metadata.xml");
            String artifactoryMetadata = IoUtils.toString(artifactoryMetadataUrl);

            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document dDoc = builder.parse(new InputSource(new StringReader(artifactoryMetadata)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            String version = (String) xpath.evaluate("/metadata/versioning/latest", dDoc, XPathConstants.STRING);

            latestVersion = new ComparableVersion((String) version);

        } catch (
            Exception error) {
            throw new GyroException(
                String.format("An error occurred."),
                error);
        }

        return latestVersion;
    }
}
