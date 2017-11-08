package beam.cli;

import beam.BeamCloud;
import beam.aws.AWSCloud;
import beam.aws.EnterpriseCredentialsProvider;
import beam.aws.EnterpriseCredentialsProviderChain;
import beam.enterprise.EnterpriseApi;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Command(name = "iam", description = "Start iam server. Defaults to port 8602.")
public class IamCommand extends AbstractGlobalCommand {

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);

    @Option(type = OptionType.GLOBAL, name = "--debug", description = "Debug mode")
    public Boolean debug;

    @Option(name = {"--port"}, description = "Port of handlers")
    public static String port;

    @Option(name = {"--config"}, description = "Location of the config file")
    public String configPath;

    @Option(name = {"--zone"}, description = "Zone to emulate")
    public String zone = "us-east-1a";

    public void run(BeamCloud cloud) {
        if (!(cloud instanceof AWSCloud)) {
            return;
        }

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        AbstractCloudCommand.getCommandConfig("iam", ServerCommand.class, this, configPath);

        if (debug == null) {
            debug = false;
        }

        if (ObjectUtils.isBlank(port)) {
            port = "8602";
        }

        if (debug) {
            root.setLevel(Level.DEBUG);
        }

        PathHandler pathHandler = new PathHandler();
        pathHandler.addPrefixPath("/latest/meta-data/iam/security-credentials", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                exchange.getResponseSender().send("vagrant");
            }
        });

        pathHandler.addPrefixPath("/latest/meta-data/placement/availability-zone", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                exchange.getResponseSender().send(zone);
            }
        });

        pathHandler.addPrefixPath("/latest/meta-data/iam/security-credentials/vagrant", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                Map<String, String> credentials = null;
                try {
                    AWSCloud awsCloud = (AWSCloud) cloud;

                    // Ensure the InstanceProfile provider is not in the chain.
                    if (awsCloud.getProvider() instanceof EnterpriseCredentialsProviderChain) {
                        EnterpriseCredentialsProviderChain chain = (EnterpriseCredentialsProviderChain) awsCloud.getProvider();
                        chain.getProviders().clear();
                        if (EnterpriseApi.isAvailable()) {
                            chain.getProviders().add(new EnterpriseCredentialsProvider(runtime.getAccount(), runtime.getProject()));
                        }
                        chain.getProviders().add(new ProfileCredentialsProvider(runtime.getAccount()));
                    }

                    credentials = cloud.findCredentials();

                } catch (beam.enterprise.EnterpriseException error) {
                    error.printStackTrace();
                } catch (beam.BeamException be) {
                    if (!"no-account".equals(be.getCode())) {
                        throw be;
                    }
                }

                Map<String, String> response = new HashMap<>();
                response.put("AccessKeyId", credentials.get("accessKeyId"));
                response.put("SecretAccessKey", credentials.get("secretKey"));
                response.put("Token", credentials.get("sessionToken"));
                response.put("Type", "AWS-HMAC");
                response.put("Code", "Success");

                if (credentials.containsKey("expiration")) {
                    Long expiration = Long.valueOf(credentials.get("expiration"));
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    df.setTimeZone(tz);
                    String expirationIso = df.format(new Date(expiration));

                    response.put("Expiration", expirationIso);
                }

                exchange.getResponseSender().send(ObjectUtils.toJson(response));
            }
        });

        Undertow server = Undertow.builder()
                .addHttpListener(Integer.parseInt(port), "127.0.0.1")
                .setHandler(pathHandler)
                .build();
        server.start();

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                return;
            }
        }
    }

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {
            @Override
            public void each(BeamCloud cloud) throws Exception {
                if (cloud instanceof AWSCloud) {
                    run(cloud);
                    System.exit(0);
                }
            }
        };
    }

}
