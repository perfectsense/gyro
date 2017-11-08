package beam.cli;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamInstance;
import beam.config.RootConfig;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;
import us.monoid.web.TextResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

import static beam.handlers.ServiceInstance.Mark;

@Command(name = "mark", description = "Mark an instance in/out of service.")
public class MarkCommand extends ListCommand implements AuditableCommand {

    private static final Table SSH_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Instance ID", 20).
            addColumn("Status", 15).
            addColumn("Environment", 15).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65);

    @Option(name = {"--available"}, description = "Mark instance in service.")
    public boolean available = false;

    @Option(name = {"--unavailable"}, description = "Mark instance out of service.")
    public boolean unavailable = false;

    @Option(name = {"--port"}, description = "Port of beam servers.")
    public String serverPort;

    public static final String MARK_API_PATH = "/v2/markInstance";

    public static final String INSTANCE_API_PATH = "/v2/instance";

    public static final String METADATA_URL = "http://169.254.169.254//latest/dynamic/instance-identity/document/";

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MarkCommand.class);

    private void callMarkService(BeamCloud cloud, BeamInstance instance, Mark mark, PrintWriter out) throws Exception {
        System.out.println(String.format("Marking instance %s %s...", instance.getId(), mark.toString()));

        Queue<String> allHosts = new LinkedList<>();

        List<String> gatewayIps = findGatewayIps(cloud, instance);
        allHosts.addAll(gatewayIps);

        boolean success = false;

        while (!allHosts.isEmpty() && !success) {
            String beamServer = allHosts.poll();

            out.println("Marking instance through: " + beamServer + "...");

            String url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + beamServer + ":" + serverPort + MARK_API_PATH,
                    "instanceId", instance.getId(),
                    "mark", mark.toString(),
                    "timeStamp", mark.getTimeStamp());

            try {
                TextResource response = new Resty().
                        setOptions(Resty.Option.timeout(10000)).
                        text(url);

                if (response.status(500)) {

                } else if (response.status(204)) {
                    System.out.println("The beam mark request has an older timeStamp.");

                } else {
                    setEverConfirmed(500);
                    success = true;
                    out.println("@|green Ok|@");
                }

            } catch (Exception ex) {
                LOGGER.info("Unable to mark instance through beam server: " + beamServer);
                out.println(ex.getMessage());
            }
        }

        if (!success) {
            LOGGER.error("Unable to mark instance through all beam servers.");
            out.println("@|red Failed|@");
        }
    }

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {

                List<String> gatewayIps = new ArrayList<>();
                gatewayIps = findGatewayIps(cloud, instances.get(0));

                if (ObjectUtils.isBlank(serverPort)) {
                    serverPort = "8601";
                }

                Map<String, Mark> statuses = fetchInstanceStatus(gatewayIps);

                Mark mark = null;
                if (available && unavailable) {
                    throw new BeamException("Only one of -in-service or -out-of-service can be selected.");
                }

                if (available) {
                    mark = Mark.AVAILABLE;
                    mark.setTimeStamp(System.currentTimeMillis());

                } else if (unavailable) {
                    mark = Mark.UNAVAILABLE;
                    mark.setTimeStamp(System.currentTimeMillis());
                }

                if (instances.size() == 1 && mark != null) {
                    callMarkService(cloud, instances.get(0), mark, out);
                } else {
                    SSH_TABLE.writeHeader(out);

                    int index = 0;

                    for (BeamInstance instance : instances) {
                        ++ index;

                        String environment = instance.getEnvironment();
                        if (instance.isSandboxed()) {
                            environment = "@|blue sandbox (" + environment + ") |@";
                        }

                        Mark m = statuses.get(instance.getId());
                        String status = "N/A";
                        if (m != null) {
                            status = m.getName();
                        }

                        SSH_TABLE.writeRow(
                                out,
                                index,
                                instance.getId(),
                                status,
                                environment,
                                instance.getLocation(),
                                instance.getLayer(),
                                instance.getState(),
                                instance.getHostname());
                    }

                    SSH_TABLE.writeFooter(out);

                    if (mark != null) {
                        out.print("\nMore than one instance matched your criteria, pick one to mark into: ");
                        out.flush();

                        BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
                        int pick = ObjectUtils.to(int.class, pickReader.readLine());

                        if (pick > instances.size() || pick <= 0) {
                            throw new BeamException(String.format(
                                    "Must pick a number between 1 and %d!",
                                    instances.size()));
                        }

                        callMarkService(cloud, instances.get(pick - 1), mark, out);
                    }
                }
            }
        };
    }

    private Map<String, Mark> fetchInstanceStatus(List<String> gatewayIps) throws Exception {

        Map<String, Mark> statuses = new HashMap<>();
        Queue<String> allHosts = new LinkedList<>();
        allHosts.addAll(gatewayIps);

        boolean success = false;

        while (!allHosts.isEmpty() && !success) {
            String beamServer = allHosts.poll();

            out.println("Fetching mark status from: " + beamServer + "...");

            String url = com.psddev.dari.util.StringUtils.addQueryParameters(
                    "http://" + beamServer + ":" + serverPort + INSTANCE_API_PATH);

            try {
                TextResource response = new Resty().
                        setOptions(Resty.Option.timeout(10000)).
                        text(url);

                Map<String, Object> json = (Map<String, Object>) ObjectUtils.fromJson(response.toString());
                if (json.get("instances") != null) {
                    List<Object> instances = (List) json.get("instances");

                    for (Object i : instances) {
                        Map<String, Object> instance = (Map<String, Object>) i;

                        if (instance.get("mark") != null) {
                            String instanceId = (String) instance.get("id");
                            Mark mark = Mark.valueOf((String) instance.get("mark"));

                            statuses.put(instanceId, mark);
                        }
                    }
                }

                success = true;

            } catch (Exception error) {
                LOGGER.info("Unable to fetch mark status from beam server: " + beamServer);
            }
        }

        if (!success) {
            LOGGER.error("Unable to fetch mark status from all beam servers.");
        }

        return statuses;
    }

    private List<String> findGatewayIps(BeamCloud cloud, BeamInstance beamInstance) throws Exception {
        List<String> gatewayIps = new ArrayList<>();

        for (BeamInstance instance : cloud.getInstances(true)) {
            if (instance.getLayer().equals("gateway") && beamInstance != instance) {
                gatewayIps.add(instance.getPrivateIpAddress());
            }
        }

        return gatewayIps;
    }
}