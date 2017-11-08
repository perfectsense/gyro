package beam.cli;

import io.airlift.command.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

import beam.BeamCloud;
import beam.BeamInstance;

@Command(name = "status", description = "Execute beam service checks on requested hosts.")
public class StatusCommand extends SshCommand {

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void each(BeamCloud cloud, BeamInstance instance) throws Exception {
                String ipAddress =  instance.getPrivateIpAddress();

                try {
                    InetAddress inet = InetAddress.getByName(ipAddress);
                    if (!inet.isReachable(500)) {
                        ipAddress = instance.getPublicIpAddress();
                    }
                } catch(Exception ex) {
                    ipAddress = instance.getPublicIpAddress();
                }

                out.println(String.format("Executing status checks on @|green %s|@:", instance.getHostname()));
                out.flush();

                try {
                    InetAddress inet = InetAddress.getByName(ipAddress);
                    if (!inet.isReachable(500)) {
                        out.println("\n@|red Instance not reachable.|@\n");
                        out.flush();

                        return;
                    }
                } catch (java.net.UnknownHostException uke) {
                    out.println("\n@|red Hostname is not resolvable. Verify you are on the VPN.|@\n");
                    out.flush();

                    return;
                }

                Process findChecksProcess = SshOptions.createProcessBuilder(sshOptions, instance, null, "-q", "ls /etc/beam/service.d/*/check.sh").start();
                BufferedReader findChecksReader = new BufferedReader(new InputStreamReader(findChecksProcess.getInputStream()));

                try {
                    for (String line; (line = findChecksReader.readLine()) != null;) {
                        out.print(String.format("%-100s", "\u2022 Executing @|yellow " + line + "|@ check..."));
                        out.flush();

                        if (SshOptions.createProcessBuilder(sshOptions, instance, null, "-q", "sudo " + line).
                                start().
                                waitFor() == 0) {
                            out.println("@|green OK|@");

                        } else {
                            out.println("@|red FAILED|@");
                        }
                    }

                } finally {
                    findChecksReader.close();
                }

                out.println();
            }
        };
    }
}
