package beam.cli;

import beam.BeamCloud;
import beam.BeamException;
import beam.BeamInstance;
import beam.BeamRuntime;
import beam.config.LayerConfig;
import beam.config.ProvisionerConfig;
import beam.config.RootConfig;

import com.psddev.dari.util.ObjectUtils;

import io.airlift.command.Command;
import io.airlift.command.Option;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

@Command(name = "provision", description = "Provision an instance or layer.")
public class ProvisionCommand extends ListCommand implements AuditableCommand {

    private static final Table PROVISION_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Instance ID", 20).
            addColumn("Environment", 18).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65);

    @Option(name = {"--ask"}, description = "Ask which host to provision.")
    public boolean ask;

    @Option(name = {"-u", "--user"}, description = "User to log in as.")
    public String user;

    @Option(name = {"-k", "--keyfile"}, description = "Private key to use (i.e. ssh -i ~/.ssh/id_rsa).")
    public String keyfile;

    @Option(name = {"--prepare"}, description = "Perform any pre-provision steps (ex: install chef).")
    public boolean prepare;

    @Option(name = {"--provisioner"}, description = "Name of the provisioner to use.")
    public String provisionerName;

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ProvisionCommand.class);

    @Override
    protected InstanceHandler getInstanceHandler() {
        final Set<String> layers = new HashSet<>();
        if (layersString != null) {
            layers.addAll(Arrays.asList(layersString.split(",")));
        }

        final Set<String> instanceIds = new HashSet<>();
        if (instanceIdsString != null) {
            instanceIds.addAll(Arrays.asList(instanceIdsString.split(",")));
        }

        final boolean includeEnv = ObjectUtils.isBlank(layersString) && ObjectUtils.isBlank(instanceIds);

        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                List<BeamInstance> provisionInstances = new ArrayList<>();
                RootConfig rootConfig = runtime.getConfig();
                Iterator<BeamInstance> instanceIterator = instances.iterator();
                while (instanceIterator.hasNext()) {
                    BeamInstance instance = instanceIterator.next();
                    LayerConfig layer = rootConfig.getLayerByName(instance.getLayer());
                    boolean containsProvisioner = false;
                    if (layer != null) {
                        if (!layer.getProvisioners().isEmpty()) {
                            if (provisionerName != null) {
                                for (ProvisionerConfig provisioner : layer.getProvisioners()) {
                                    if (provisioner.getName() != null && provisioner.getName().equals(provisionerName)) {
                                        containsProvisioner = true;
                                    }
                                }
                                if (!containsProvisioner) {
                                    instanceIterator.remove();
                                }
                            }
                        } else {
                            instanceIterator.remove();
                        }
                    } else if (instance.getLayer().equals("gateway")) {
                        List<ProvisionerConfig> provisioners = rootConfig.getNetworkConfig().getGatewayProvisioners(instance.getLocation());
                        if (provisioners != null && provisioners.size() > 0) {
                            if (provisionerName != null) {
                                for (ProvisionerConfig provisioner : rootConfig.getNetworkConfig().getGatewayProvisioners(instance.getLocation())) {
                                    if (provisioner.getName() != null && provisioner.getName().equals(provisionerName)) {
                                        containsProvisioner = true;
                                    }
                                }
                                if (!containsProvisioner) {
                                    instanceIterator.remove();
                                }
                            }
                        } else {
                            instanceIterator.remove();
                        }
                    }
                }
                if (instances.isEmpty()) {
                    throw new BeamException(String.format("No instances with required provisioners found!"));
                }
                String provisionerPrompt = provisionerName == null ? "" : " with the provisioner \"" + provisionerName + "\"";
                if (ask) {
                    PROVISION_TABLE.writeHeader(out);

                    int index = 0;

                    for (BeamInstance instance : instances) {
                        ++ index;

                        String environment = instance.getEnvironment();
                        if (runtime.getConfig().getNetworkConfig().isSandbox()) {
                            environment = "@|green sandbox (" + environment + ") |@";
                        }

                        PROVISION_TABLE.writeRow(
                                out,
                                index,
                                instance.getId(),
                                environment,
                                instance.getLocation(),
                                instance.getLayer(),
                                instance.getState(),
                                instance.getHostname());
                    }

                    PROVISION_TABLE.writeFooter(out);
                    out.print("\nMore than one instance matched your criteria, pick one to provision" + provisionerPrompt + ": ");
                    out.flush();

                    BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
                    int pick = ObjectUtils.to(int.class, pickReader.readLine());

                    if (pick > instances.size() || pick <= 0) {
                        throw new BeamException(String.format(
                                "Must pick a number between 1 and %d!",
                                instances.size()));
                    }

                    provisionInstances.add(instances.get(pick - 1));
                } else {
                    for (BeamInstance instance : instances) {
                        if (!instance.getState().equals("running") &&
                                !instance.getState().equals("ACTIVE") &&
                                !instance.getState().equals("VM running")) {
                            continue;
                        }

                        LOGGER.debug("Checking layer: {}", instance.getLayer());
                        if (layers.contains(instance.getLayer())) {
                            provisionInstances.add(instance);
                        }

                        LOGGER.debug("Checking instance: {}", instance.getId());
                        if (instanceIds.contains(instance.getId()) || includeEnv) {
                            provisionInstances.add(instance);
                        }
                    }
                }

                if (provisionInstances.size() > 0) {

                    if (!ask) {
                        out.println("The following instances will be provisioned:");
                        out.println("");

                        for (BeamInstance instance : provisionInstances) {
                            out.format("- %s %s serial-%s %s [%s] %s\n",
                                    runtime.getProject(),
                                    runtime.getEnvironment(),
                                    runtime.getSerial(),
                                    instance.getLayer(),
                                    instance.getRegion(), instance.getId());
                        }

                        out.format("\nAre you sure you want to provision these instances in @|blue %s|@ cloud" + provisionerPrompt + " (y/N) ", cloud.getName());
                        out.flush();

                        BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                        if (!"y".equalsIgnoreCase(confirmReader.readLine())) {
                            return;
                        }
                    }

                    setEverConfirmed(0);
                    if (keyfile != null) {
                        keyfile = new File(keyfile).getAbsolutePath();
                    }

                    for (BeamInstance instance : provisionInstances) {
                        LayerConfig layer = rootConfig.getLayerByName(instance.getLayer());
                        if (layer != null) {
                            if (!layer.getProvisioners().isEmpty()) {
                                for (ProvisionerConfig provisioner : layer.getProvisioners()) {
                                    if (provisionerName != null) {
                                        if (provisioner.getName() != null && provisioner.getName().equals(provisionerName)) {
                                            provisioner.setKeyfile(keyfile);
                                            provisioner.setUser(user);
                                            provisioner.provision(instance, rootConfig, prepare, out);
                                        } else {
                                            String currentProvisionerName = provisioner.getName() == null ? "Unnamed" : provisioner.getName();
                                            out.println("Provisioner " + currentProvisionerName + " skipped due to --provisioner");
                                        }
                                    } else {
                                        provisioner.setKeyfile(keyfile);
                                        provisioner.setUser(user);
                                        provisioner.provision(instance, rootConfig, prepare, out);
                                    }
                                }
                            } else {
                                out.println("No provisioners detected.");
                            }
                        } else if (instance.getLayer().equals("gateway")) {
                            List<ProvisionerConfig> provisioners = rootConfig.getNetworkConfig().getGatewayProvisioners(instance.getLocation());
                            if (provisioners != null && provisioners.size() > 0) {
                                for (ProvisionerConfig provisioner : rootConfig.getNetworkConfig().getGatewayProvisioners(instance.getLocation())) {
                                    if (provisionerName != null) {
                                        if (provisioner.getName() != null && provisioner.getName().equals(provisionerName)) {
                                            provisioner.setKeyfile(keyfile);
                                            provisioner.setUser(user);
                                            provisioner.provision(instance, rootConfig, prepare, out);
                                        } else {
                                            String currentProvisionerName = provisioner.getName() == null ? "Unnamed" : provisioner.getName();
                                            out.println("Provisioner " + currentProvisionerName + " skipped due to --provisioner");
                                        }
                                    } else {
                                        provisioner.setKeyfile(keyfile);
                                        provisioner.setUser(user);
                                        provisioner.provision(instance, rootConfig, prepare, out);
                                    }
                                }
                            } else {
                                out.println("No provisioners detected.");
                            }
                        }

                        out.println();
                    }
                } else if (provisionInstances.size() == 0) {
                    out.write("@|red No instances found to provision.|@\n");
                    out.flush();
                }
            }

        };
    }

}