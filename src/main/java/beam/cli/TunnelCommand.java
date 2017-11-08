package beam.cli;

import beam.BeamCloud;
import beam.BeamInstance;
import io.airlift.command.Command;
import io.airlift.command.Option;

import javax.inject.Inject;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Command(name = "tunnel", description = "Tunnel to a running instance.")
public class TunnelCommand extends ListCommand {

    private static final Table TUNNEL_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Instance ID", 20).
            addColumn("Environment", 15).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65);

    @Option(name = { "--localPort" }, description = "Local port to listen on.")
    public Integer localPort;

    @Option(name = { "--remotePort" }, description = "Remote port to connect to.")
    public Integer remotePort;

    @Option(name = { "--nobrowser" }, description = "Don't open browser automatically.")
    public boolean noBrowser;

    @Inject
    public SshOptions sshOptions;

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                if (sshOptions == null) {
                    sshOptions = new SshOptions();
                }
                sshOptions.useGateway = true;

                BeamInstance instance = null;
                if (instances.size() > 1) {
                    instance = SshCommand.pickInstance(out, instances);
                } else if (instances.size() != 0){
                    instance = instances.get(0);
                }

                BeamInstance gateway = SshCommand.pickNearestGateway(cloud, instance, sshOptions);
                ProcessBuilder processBuilder = tunnel(instance, gateway);

                out.write("Tunneling local port " + localPort + " to " + remotePort + " on " + instance.getId());
                out.write("\n\nhttp://localhost:" + localPort);
                out.write("\n");
                out.flush();

                Process process = processBuilder.inheritIO().start();

                if (!noBrowser) {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + localPort));
                }

                process.waitFor();
            }
        };
    }

    private ProcessBuilder tunnel(BeamInstance instance, BeamInstance gateway) {
        List<String> arguments = new ArrayList<>();

        String remoteHost = instance.getPrivateIpAddress();
        String gatewayHost = gateway.getPublicIpAddress();

        if (localPort == null) {
            localPort = 4000;
        }

        if (remotePort == null) {
            remotePort = 8080;
        }

        arguments.add("ssh");
        arguments.add("-nNT");

        if (sshOptions != null && sshOptions.keyfile != null) {
            arguments.add("-i ");
            arguments.add(sshOptions.keyfile);
        }

        arguments.add("-L");
        arguments.add(localPort + ":" + remoteHost + ":" + remotePort);

        arguments.add("-o");
        arguments.add("StrictHostKeychecking=no");

        arguments.add("-o");
        arguments.add("ExitOnForwardFailure=yes");

        if (sshOptions != null && sshOptions.user != null) {
            gatewayHost = String.format("%s@%s", sshOptions.user, gateway.getPublicIpAddress());
        }
        arguments.add(gatewayHost);

        return new ProcessBuilder(arguments);
    }

}
