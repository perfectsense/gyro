package beam.cli;

import beam.BeamCloud;
import beam.BeamInstance;
import io.airlift.command.Command;
import io.airlift.command.Option;

import javax.inject.Inject;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Command(name = "copy", description = "Copy a file to running instance(s).")
public class CopyCommand extends ListCommand implements AuditableCommand {

    @Option(name = { "--src" }, required = true, description = "Local file to copy.")
    public String source;

    @Option(name = { "--dest" }, required = true, description = "Location to copy file to on remote host.")
    public String destination;

    @Inject
    public SshOptions sshOptions;

    @Override
    protected InstanceHandler getInstanceHandler() {


        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                setEverConfirmed(0);
                for (BeamInstance instance : instances) {
                    String hostname =  instance.getPrivateIpAddress();

                    try {
                        InetAddress inet = InetAddress.getByName(hostname);
                        if (!inet.isReachable(500)) {
                            hostname = instance.getPublicIpAddress();
                        }
                    } catch (Exception ex) {
                        hostname = instance.getPublicIpAddress();
                    }

                    List<String> arguments = new ArrayList<>();

                    arguments.add("scp");

                    if (sshOptions != null && sshOptions.keyfile != null) {
                        arguments.add("-i");
                        arguments.add(sshOptions.keyfile);
                    }

                    arguments.add(source);

                    if (sshOptions != null && sshOptions.user != null) {
                        arguments.add(String.format("%s@%s:%s", sshOptions.user, hostname, destination));

                    } else {
                        arguments.add(hostname + ":" + destination);
                    }

                    String sandbox = instance.isSandboxed() ? "@|blue (sandbox)|@" : "";
                    out.format("Copying @|green %s|@ to @|green %s|@ on @|yellow %s|@ %s...\n",
                            source, destination, instance.getHostname(), sandbox);
                    out.flush();

                    int exitCode = new ProcessBuilder(arguments).inheritIO().start().waitFor();

                    if (exitCode != 0) {
                        out.write("@|red Copy failed!|@\n");
                        out.flush();
                        return;
                    }
                }
            }
        };
    }


}
