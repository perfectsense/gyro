package beam.cli;

import beam.BeamCloud;
import beam.BeamException;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Properties;
import java.nio.file.Paths;
import org.ini4j.Wini;

@Command(name = "credentials", description = "Print cloud credentials.")
public class CredentialsCommand extends AbstractGlobalCommand {

    @Option(name = { "-f", "--format" }, description = "Output format.")
    public Format format;

    @Option(name = { "--refresh" }, description = "Refresh default credentials.")
    public boolean refresh;

    @Option(name = { "--extended" }, description = "Get extended credentials with no IAM access.")
    public boolean extended;

    @Option(name = { "-c", "--cloud" }, description = "Cloud name.")
    public String cloudName;

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {

            @Override
            public void last(Set<BeamCloud> clouds) throws Exception {
                Map<String, Map<String, String>> credentialsMap = new HashMap<>();

                for (BeamCloud cloud : clouds) {
                    Map<String, String> credentials = null;
                    try {
                        credentials = cloud.findCredentials(refresh, extended);

                    } catch (beam.enterprise.EnterpriseException error) {

                    } catch (beam.BeamException be) {
                        if (!"no-account".equals(be.getCode())) {
                            throw be;
                        }
                    }

                    if (ObjectUtils.isBlank(cloudName) || cloudName.equals(cloud.getName())) {
                        if (credentials != null) {
                            credentialsMap.put(cloud.getName(), credentials);
                        }
                    }

                    if (refresh && "ec2".equals(cloud.getName())) {
                        refreshCredentials(credentials);
                    }
                }

                if (format == null) {
                    format = Format.JSON;
                }

                format.print(out, credentialsMap);
                out.flush();
            }
        };
    }

    private void refreshCredentials(Map<String, String> credentials) throws Exception {
        File credentialsFile = Paths.get(System.getProperty("user.home"), ".aws", "credentials").toFile();

        if (!credentialsFile.exists()) {
            credentialsFile.createNewFile();
        }

        Wini ini = new Wini(credentialsFile);
        String section = runtime.getAccount();

        String exist = ini.get(section, "aws_secret_access_key", String.class);
        boolean refresh;

        if (ObjectUtils.isBlank(exist)) {
            refresh = true;

        } else {
            refresh = ini.get(section, "beam.refresh", boolean.class);
        }

        if (refresh) {
            out.println("@|green Refreshing account " + runtime.getAccount() + "|@");

            ini.put(section, "beam.refresh", true);
            ini.put(section, "aws_secret_access_key", credentials.get("secretKey"));
            ini.put(section, "aws_access_key_id", credentials.get("accessKeyId"));

            if (credentials.get("sessionToken") != null) {
                ini.put(section, "aws_session_token", credentials.get("sessionToken"));
            }

            ini.store();
        } else {
            out.println("@|red Skipping refresh of account " + runtime.getAccount() + ". To refresh these credentials add 'beam.refresh=true' to this accounts section in ~/.aws/credentials|@");
        }
    }

    public enum Format {

        JSON() {

            @Override
            public void print(PrintWriter out, Map<String, Map<String, String>> credentialsMap) throws IOException {
                out.println();
                out.println(ObjectUtils.toJson(credentialsMap));
            }
        },

        PROPERTIES() {

            @Override
            public void print(PrintWriter out, Map<String, Map<String, String>> credentialsMap) throws IOException {
                for (String name : credentialsMap.keySet()) {

                    out.println("\ncloud: " + name);
                    Map<String, String> credentials = credentialsMap.get(name);

                    Properties props = new Properties();

                    props.putAll(credentials);
                    props.store(out, null);
                }
            }
        };

        public abstract void print(PrintWriter out, Map<String, Map<String, String>> credentialsMap) throws IOException;
    }
}
