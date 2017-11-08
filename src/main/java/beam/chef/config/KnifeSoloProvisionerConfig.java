package beam.chef.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beam.BeamInstance;
import beam.BeamRuntime;
import beam.cli.SshOptions;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import beam.config.ConfigValue;
import beam.config.ProvisionerConfig;
import beam.config.RootConfig;

@ConfigValue("knife-solo")
public class KnifeSoloProvisionerConfig extends ProvisionerConfig {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KnifeSoloProvisionerConfig.class);

    private String recipe;
    private List<String> recipes;
    private String cookbookPath;
    private String environment;
    private List<String> roles;
    private String version;

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getCookbookPath() {
        return cookbookPath;
    }

    public void setCookbookPath(String cookbookPath) {
        this.cookbookPath = cookbookPath;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<>();
        }

        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getRecipes() {
        if (recipes == null) {
            recipes = new ArrayList<>();

            if (!ObjectUtils.isBlank(recipe)) {
                recipes.add(recipe);
            }
        }

        return recipes;
    }

    public void setRecipes(List<String> recipes) {
        this.recipes = recipes;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String resolveHost(List<String> hostnames) {
        for (String hostname : hostnames) {
            try {
                InetAddress inet = InetAddress.getByName(hostname);
                if (SshOptions.hasService(inet, 22)) {
                    return hostname;
                }
            } catch (IOException ioe) {

            }
        }

        return null;
    }

    @Override
    public void provision(BeamInstance instance, RootConfig config, boolean prepare, PrintWriter out) throws Exception {
        String hostname =  resolveHost(Arrays.asList(
                instance.getHostname(),
                instance.getPrivateIpAddress(),
                instance.getPublicIpAddress()));

        if (hostname == null) {
            out.format("Unable to determine host IP of %s. Verify you are on the VPN and/or have security group access.\n", instance.getHostname());
            out.flush();
            return;
        }

        String command = hostname;
        if (getUser() != null) {
            command = String.format("%s@%s", getUser(), hostname);
        }

        if (environment == null) {
            environment = config.getEnvironment();
        }

        if (environment.equals("prod")) {
            environment = "production";
        } else if (environment.equals("dev")) {
            environment = "development";
        }

        if (prepare && executePrepare(hostname, command, instance.isSandboxed(), out) != 0) {
            throw new RuntimeException("Preparing failed.");
        }

        if (executeProvision(hostname, command, instance.isSandboxed(), out) != 0) {
            throw new RuntimeException("Provisioning failed.");
        }
    }

    public int executePrepare(String hostname, String command, boolean sandboxed, PrintWriter out) throws Exception {
        String sandbox = sandboxed ? "@|blue (sandbox)|@" : "";
        out.format("Preparing '@|green %s|@' with @|yellow %s|@ %s...\n\n", hostname, getType(), sandbox).flush();

        List<String> arguments = new ArrayList<>();
        arguments.add("knife");
        arguments.add("solo");
        arguments.add("prepare");
        arguments.add("--bootstrap-version");
        arguments.add(getVersion() != null ? getVersion() : "12.3.0");
        if (getKeyfile() != null) {
            arguments.add("-i");
            arguments.add(getKeyfile());
        }
        arguments.add(command);

        LOGGER.debug("Preparing using: {}", StringUtils.join(arguments, " "));

        ProcessBuilder pb = new ProcessBuilder(arguments).directory(new File(getCookbookPath()));

        pb.environment().remove("BRIGHTSPOT_CHEF");

        Process process = BeamRuntime.startAuditSubprocess(pb);
        int exitValue = process.waitFor();

        return exitValue;
    }

    public int executeProvision(String hostname, String command, boolean sandboxed, PrintWriter out) throws Exception {
        String sandbox = sandboxed ? "@|blue (sandbox)|@" : "";
        out.format("Provisioning '@|green %s|@' with @|yellow %s|@ %s...\n\n", hostname, getType(), sandbox).flush();

        List<String> arguments = new ArrayList<>();
        arguments.add("knife");
        arguments.add("solo");
        arguments.add("cook");
        if (getKeyfile() != null) {
            arguments.add("-i");
            arguments.add(getKeyfile());
        }
        arguments.add("--no-chef-check");

        List<String> runList = new ArrayList<>();
        for (String r : getRecipes()) {
            runList.add(String.format("recipe[%s]", r));
        }

        for (String r : getRoles()) {
            runList.add(String.format("role[%s]", r));
        }

        arguments.add("-o");
        arguments.add(StringUtils.join(runList, ","));

        arguments.add("-E");
        arguments.add(environment);
        arguments.add(command);

        LOGGER.debug("Provisioning using: " + StringUtils.join(arguments, " "));

        ProcessBuilder pb = new ProcessBuilder(arguments).directory(new File(getCookbookPath()));

        pb.environment().remove("BRIGHTSPOT_CHEF");

        Process process = BeamRuntime.startAuditSubprocess(pb);
        int exitValue = process.waitFor();

        return exitValue;
    }

}