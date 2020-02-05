package gyro.core.command;

import java.io.FileNotFoundException;
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
    protected void doExecute() throws Exception {
        VersionCommand.getLatestVersion();
    }

    public static void getLatestVersion() throws FileNotFoundException {
        String version;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

        InputStream stream = VersionCommand.class.getResourceAsStream("/gyro.properties");
        Properties properties = new Properties();

        try {
            URL artifactoryMetadataUrl = new URL(
                "https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-osx/maven-metadata.xml");
            String artifactoryMetadata = IoUtils.toString(artifactoryMetadataUrl);

            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document dDoc = builder.parse(new InputSource(new StringReader(artifactoryMetadata)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            version = (String) xpath.evaluate("/metadata/versioning/latest", dDoc, XPathConstants.STRING);
            properties.load(stream);

            ComparableVersion currentVersion = new ComparableVersion((String) properties.get("version"));
            ComparableVersion latestVersion = new ComparableVersion((String) version);

            GyroCore.ui().write("@|green Gyro version: %s|@\n", currentVersion);
            // GyroCore.ui().write("@|yellow + comparison: %s|@\n", currentVersion.compareTo(latestVersion));
            if (currentVersion.compareTo(latestVersion) < 0) {
                GyroCore.ui().write("@|blue Latest version: %s|@\n", latestVersion);
                GyroCore.ui()
                    .write(
                        "@|red Download the latest version from:\n %s|@\n",
                        "https://artifactory.psdops.com/gyro-releases/gyro/gyro-cli-osx/" + latestVersion
                            + "/gyro-cli-osx-"
                            + latestVersion + ".zip");
            }
        } catch (Exception ex) {
            System.out.println("error");
        }
    }
}
