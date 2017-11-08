package beam.cli;

import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamInstance;

import com.psddev.dari.util.ObjectUtils;

@Command(name = "ssh", description = "SSH to a running instance.")
public class SshCommand extends ListCommand {

    private static final Table SSH_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Instance ID", 20).
            addColumn("Environment", 15).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65);

    @Option(name = { "-e", "--execute" }, description = "Command to execute on host(s).")
    public String command;

    @Option(name = { "-c", "--continue" }, description = "Ignore exit code and continue running -e command to run on all hosts.")
    public boolean force;

    @Option(name = { "--tmux" }, description = "Open a tmux session with each host.")
    public boolean useTmux;

    @Inject
    public SshOptions sshOptions;

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                if (command != null) {
                    for (BeamInstance instance : instances) {
                        String sandbox = instance.isSandboxed() ? "@|blue (sandbox)|@" : "";
                        out.format("Executing @|green %s|@ on @|yellow %s|@ %s...\n", command, instance.getHostname(), sandbox);
                        out.flush();

                        int exitCode = SshOptions.createProcessBuilder(sshOptions, instance, pickNearestGateway(cloud, instance, sshOptions), command).
                                inheritIO().start().waitFor();

                        if (exitCode != 0 && !force) {
                            out.write("@|red Command failed!|@\n");
                            out.flush();
                            return;
                        }
                    }
                } else if (useTmux) {
                    String tmuxScript = "#!/bin/sh\n";
                    tmuxScript += "SESSION=`tmux new-session -d -P`\n";

                    for (BeamInstance instance : instances) {
                        List<String> arguments = SshOptions.createArgumentsList(sshOptions, instance, pickNearestGateway(cloud, instance, sshOptions));

                        String sshCommand = "";
                        for (String arg : arguments) {
                            if (arg.contains(" ")) {
                               sshCommand += "\'" + arg + "\'";
                            } else {
                                sshCommand += arg;
                            }

                            sshCommand += " ";
                        }

                        tmuxScript += "tmux new-window -t ${SESSION}" +
                                " -n " + instance.getLayer() + ":" + instance.getId() +
                                " -- " + sshCommand + "\n";
                    }

                    tmuxScript += "tmux kill-window -t ${SESSION}1\n";
                    tmuxScript += "tmux move-window -rt ${SESSION}\n";
                    tmuxScript += "tmux attach-session -t ${SESSION}\n";

                    File temp = File.createTempFile("tmux", "beam");
                    temp.setExecutable(true);
                    temp.deleteOnExit();

                    Writer out = new FileWriter(temp);
                    out.write(tmuxScript);
                    out.close();

                    new ProcessBuilder(temp.toString()).inheritIO().start().waitFor();
                } else if (instances.size() == 1) {
                    BeamInstance instance = instances.get(0);
                    BeamInstance gateway = sshOptions != null && sshOptions.useGateway ? pickNearestGateway(cloud, instance, sshOptions) : null;

                    SshOptions.createProcessBuilder(sshOptions, instance, gateway).
                            inheritIO().
                            start().
                            waitFor();

                } else {
                    BeamInstance instance = pickInstance(out, instances);
                    BeamInstance gateway = sshOptions != null && sshOptions.useGateway ? pickNearestGateway(cloud, instance, sshOptions) : null;

                    SshOptions.createProcessBuilder(sshOptions, instance, gateway).
                            inheritIO().
                            start().
                            waitFor();
                }
            }
        };
    }

    static BeamInstance pickInstance(PrintWriter out, List<BeamInstance> instances) throws IOException {
        SSH_TABLE.writeHeader(out);

        int index = 0;

        for (BeamInstance instance : instances) {
            ++ index;

            String environment = instance.getEnvironment();
            if (instance.isSandboxed()) {
                environment = "@|blue sandbox (" + environment + ") |@";
            }

            SSH_TABLE.writeRow(
                    out,
                    index,
                    instance.getId(),
                    environment,
                    instance.getLocation(),
                    instance.getLayer(),
                    instance.getState(),
                    instance.getHostname());
        }

        SSH_TABLE.writeFooter(out);
        out.print("\nMore than one instance matched your criteria, pick one to log into: ");
        out.flush();

        BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
        int pick = ObjectUtils.to(int.class, pickReader.readLine());

        if (pick > instances.size() || pick <= 0) {
            throw new BeamException(String.format(
                    "Must pick a number between 1 and %d!",
                    instances.size()));
        }

        return instances.get(pick - 1);
    }

    static BeamInstance pickNearestGateway(BeamCloud cloud, BeamInstance beamInstance, SshOptions options) throws Exception {
        BeamInstance gateway = null;

        if (options != null && options.useGateway) {
            List<BeamInstance> gateways = new ArrayList<>();
            for (BeamInstance instance : cloud.getInstances(true)) {
                if (instance.getLayer() != null && instance.getLayer().equals("gateway") && beamInstance != instance) {
                    gateways.add(instance);

                    if (instance.getLocation().equals(beamInstance.getLocation())) {
                        gateway = instance;
                        break;
                    }
                }
            }

            if (gateway == null && !gateways.isEmpty()) {
                gateway = gateways.get(0);
            }
        }

        return gateway;
    }

}
