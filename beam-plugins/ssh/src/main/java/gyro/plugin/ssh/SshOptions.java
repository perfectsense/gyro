package gyro.plugin.ssh;

import beam.core.BeamException;
import beam.core.BeamInstance;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SshOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshOptions.class);

    @Option(name = {"-u", "--user"}, description = "User to log in as.")
    public String user;

    @Option(name = {"-k", "--keyfile"}, description = "Private key to use (i.e. ssh -i ~/.ssh/id_rsa).")
    public String keyfile;

    @Option(name = {"-q", "--quiet"}, description = "Quiet mode.")
    public boolean quiet;

    @Option(name = {"-o", "--options"}, description = "Options pass to ssh -o option.")
    public String options;

    public static List<String> createArgumentsList(SshOptions sshOptions, BeamInstance instance, String... additionalArguments) throws Exception {
        String hostname = instance.getPublicIpAddress();

        if (hostname == null) {
            hostname = instance.getPrivateIpAddress();
        }

        if (hostname == null) {
            throw new BeamException("Unable to determine instance's IP.");
        }

        List<String> arguments = new ArrayList<>();

        arguments.add("ssh");

        if (sshOptions != null && sshOptions.keyfile != null) {
            arguments.add("-i");
            arguments.add(sshOptions.keyfile);
        }

        if (sshOptions != null && sshOptions.quiet) {
            arguments.add("-q");
        }

        if (sshOptions != null && sshOptions.user != null) {
            arguments.add(String.format("%s@%s", sshOptions.user, hostname));

        } else {
            arguments.add(hostname);
        }

        if (sshOptions != null && sshOptions.options != null) {
            for (String option : Arrays.asList(sshOptions.options.split(","))) {
                arguments.add("-o");
                arguments.add(option);
            }
        }

        if (additionalArguments != null) {
            Collections.addAll(arguments, additionalArguments);
        }

        return arguments;
    }

    public static ProcessBuilder createProcessBuilder(SshOptions sshOptions, BeamInstance instance, String... additionalArguments) throws Exception {
        return new ProcessBuilder(createArgumentsList(sshOptions, instance, additionalArguments));
    }

    public static boolean hasService(InetAddress host, int port) {
        Socket sock = new Socket();

        try {
            sock.setSoTimeout(5000);
            sock.connect(new InetSocketAddress(host, port), 1000);
            if (sock.isConnected()) {
                byte[] buffer = new byte[3];
                if (sock.getInputStream().read(buffer) != 3) {
                    return false;
                }

                if (new String(buffer).equalsIgnoreCase("ssh")) {
                    return true;
                }

            }
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                sock.close();
            } catch (IOException ioe) {
                // Ignore
            }
        }

        return false;
    }

}

