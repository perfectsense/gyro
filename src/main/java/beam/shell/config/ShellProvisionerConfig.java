package beam.shell.config;

import beam.BeamInstance;
import beam.config.ConfigValue;
import beam.config.ProvisionerConfig;
import beam.config.RootConfig;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.PrintWriter;

@ConfigValue("shell")
public class ShellProvisionerConfig extends ProvisionerConfig {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShellProvisionerConfig.class);

    private List<String> scripts;
    private String scriptPath;
    private List<String> inlines;
    private String shebang;
    boolean sudo;

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public List<String> getScripts() {
        if (scripts == null) {
            scripts = new ArrayList<>();
        }

        return scripts;
    }

    public void setScripts(List<String> scripts) {
        this.scripts = scripts;
    }

    public List<String> getInlines() {
        if (inlines == null) {
            inlines = new ArrayList<>();
        }

        return inlines;
    }

    public void setInlines(List<String> inlines) {
        this.inlines = inlines;
    }

    public boolean getSudo() {
        return sudo;
    }

    public void setSudo(boolean sudo) {
        this.sudo = sudo;
    }

    public String getShebang() {
        return shebang;
    }

    public void setShebang(String shebang) {
        this.shebang = shebang;
    }

    @Override
    public void provision(BeamInstance instance, RootConfig config, boolean prepare, PrintWriter out) throws Exception {
        String hostname =  instance.getHostname();

        try {
            InetAddress inet = InetAddress.getByName(hostname);
            if (!inet.isReachable(500)) {
                hostname = instance.getPublicIpAddress();
            }
        } catch(Exception ex) {
            hostname = instance.getPublicIpAddress();
        }

        if (hostname == null) {
            out.format("Unable to determine host IP of %s. Verify you are on the VPN and/or have security group access.\n", instance.getHostname());
            out.flush();
            return;
        }

        String command = hostname;
        if (getUser() != null) {
            command = String.format("%s@%s", getUser(), hostname);
        }

        if (executeProvision(hostname, command, instance.isSandboxed(), out) != 0) {
            throw new RuntimeException("Provisioning failed.");
        }
    }

    public int executeProvision(String hostname, String command, boolean sandboxed, PrintWriter out) throws Exception {
        String sandbox = sandboxed ? "@|blue (sandbox)|@" : "";
        out.format("Provisioning '@|green %s|@' with @|yellow %s|@ %s...\n\n", hostname, getType(), sandbox).flush();

        if (getInlines().isEmpty() && getScripts().isEmpty()) {
            return 0;
        }

        List<String> arguments = new ArrayList<>();
        List<String> shellCommand = new ArrayList<>();

        arguments.add("/bin/bash");
        arguments.add("-c");

        shellCommand.add("{");

        if (getSudo()) {
            shellCommand.add("echo");
            shellCommand.add("sudo -s;");
        }

        if (getShebang() == null) {
            setShebang("/bin/bash");
        }

        if (!getInlines().isEmpty()) {
            shellCommand.add("echo");
            shellCommand.add("'#!" + getShebang() + "'; ");
        }

        for (String inline : getInlines()) {
            shellCommand.add("echo");
            shellCommand.add(inline + "; ");
        }

        for (String script : getScripts()) {
            shellCommand.add("cat");
            shellCommand.add(script + "; echo;");
        }

        shellCommand.add("}");
        shellCommand.add("|");

        shellCommand.add("ssh");
        shellCommand.add(command);

        if (getKeyfile() != null) {
            shellCommand.add("-i");
            shellCommand.add(getKeyfile());
        }

        shellCommand.add("/bin/bash");
        String shellString = StringUtils.join(shellCommand, " ");
        arguments.add(shellString);

        LOGGER.debug("Provisioning using: {}", StringUtils.join(arguments, " "));

        if (getScriptPath() == null) {
            setScriptPath(".");
        }

        ProcessBuilder pb = new ProcessBuilder(arguments).directory(new File(getScriptPath()));

        pb.inheritIO();
        Process process = pb.start();

        return process.waitFor();
    }
}