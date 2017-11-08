package beam.cli;

import beam.BeamException;
import io.airlift.command.Option;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import beam.BeamInstance;

public class SshOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshOptions.class);

    @Option(name = { "-u", "--user" }, description = "User to log in as.")
    public String user;

    @Option(name = { "-k", "--keyfile" }, description = "Private key to use (i.e. ssh -i ~/.ssh/id_rsa).")
    public String keyfile;

    @Option(name = { "-g", "--gateway" }, description = "Jump through the gateway host.")
    public boolean useGateway;

    @Option(name = { "-q", "--quiet" }, description = "Quiet mode.")
    public boolean quiet;

    @Option(name = { "-o", "--options" }, description = "Options pass to ssh -o option.")
    public String options;

    public static List<String> createArgumentsList(SshOptions sshOptions, BeamInstance instance, BeamInstance gateway, String... additionalArguments) throws Exception {
        String hostname = instance.getPrivateIpAddress();

        if (sshOptions == null || !sshOptions.useGateway) {
            try {
                InetAddress inet = InetAddress.getByName(hostname);
                if (!hasService(inet, 22)) {
                    hostname = instance.getPublicIpAddress();
                }
            } catch (Exception ex) {
                hostname = instance.getPublicIpAddress();
            }
        }

        if (sshOptions != null && sshOptions.useGateway && gateway == null) {
            throw new BeamException("A gateway is required to use -g. No gateway could be found.");
        }

        if (hostname == null) {
            throw new BeamException("Unable to resolve hostname. Make sure you are on the VPN or specify -g to use the gateway as a jump host.");
        }

        List<String> arguments = new ArrayList<>();

        arguments.add("ssh");

        if (sshOptions != null && sshOptions.useGateway) {
            arguments.add("-o");
            arguments.add("ForwardAgent yes");

            sshOptions.quiet = true;
        }

        if (sshOptions != null && sshOptions.keyfile != null) {
            arguments.add("-i");
            arguments.add(sshOptions.keyfile);
        }

        if (sshOptions != null && sshOptions.useGateway) {
            String KEY_FILE = "";
            String REMOTE_HOST = gateway.getPublicIpAddress();
            if (REMOTE_HOST == null) {
                throw new BeamException("Unable to determine the public ip address of the gateway.");
            }

            if (sshOptions != null && sshOptions.user != null) {
                REMOTE_HOST = String.format("%s@%s", sshOptions.user, gateway.getPublicIpAddress());
            }

            if (sshOptions != null && sshOptions.keyfile != null) {
                KEY_FILE = "-i " + sshOptions.keyfile;
            }

            arguments.add("-o");
            arguments.add("ProxyCommand ssh {KEY_FILE} -W %h:%p {REMOTE_HOST}".
                    replace("{REMOTE_HOST}", REMOTE_HOST).
                    replace("{KEY_FILE}", KEY_FILE));

            arguments.add("-o");
            arguments.add("StrictHostKeychecking=no");
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

    public static ProcessBuilder createProcessBuilder(SshOptions sshOptions, BeamInstance instance, BeamInstance gateway, String... additionalArguments) throws Exception {
        return new ProcessBuilder(createArgumentsList(sshOptions, instance, gateway, additionalArguments));
    }

    public static boolean hasService(InetAddress host, int port) {
        Socket sock = new Socket();

        try {
            sock.setSoTimeout(5000);
            sock.connect(new InetSocketAddress(host, port), 1000);
            if (sock.isConnected()) {
                byte buffer[] = new byte[3];
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

            }
        }

        return false;
    }
}
