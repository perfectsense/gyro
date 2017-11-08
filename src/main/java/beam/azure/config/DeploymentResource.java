package beam.azure.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import org.fusesource.jansi.AnsiRenderWriter;

public class DeploymentResource extends AzureResource<Void> {

    private List<AzureGroupResource> autoscaleGroups;
    private String image;
    private String instanceType;
    private String groupHash;
    private String deploymentString;
    private BeamReference domain;
    private List<AzureGroupResource> currentGroups;
    private List<LoadBalancerResource> currentElbs;
    private ZoneResource zoneResource;

    private transient Map<String, List<String>> state;

    public List<AzureGroupResource> getAutoscaleGroups() {
        if (autoscaleGroups == null) {
            autoscaleGroups = new ArrayList<>();
        }

        return autoscaleGroups;
    }

    public void setAutoscaleGroups(List<AzureGroupResource> autoscaleGroups) {
        this.autoscaleGroups = autoscaleGroups;
    }

    public List<AzureGroupResource> getCurrentGroups() {
        if (currentGroups == null) {
            currentGroups = new ArrayList<>();
        }

        return currentGroups;
    }

    public void setCurrentGroups(List<AzureGroupResource> currentGroups) {
        this.currentGroups = currentGroups;
    }

    public List<LoadBalancerResource> getCurrentElbs() {
        if (currentElbs == null) {
            currentElbs = new ArrayList<>();
        }

        return currentElbs;
    }

    public void setCurrentElbs(List<LoadBalancerResource> currentElbs) {
        this.currentElbs = currentElbs;
    }

    public ZoneResource getHostedZone() {
        return zoneResource;
    }

    public void setZoneResource(ZoneResource zoneResource) {
        this.zoneResource = zoneResource;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @ResourceDiffProperty
    public String getGroupHash() {
        return groupHash;
    }

    public void setGroupHash(String groupHash) {
        this.groupHash = groupHash;
    }

    public String getDeploymentString() {
        return deploymentString;
    }

    public void setDeploymentString(String deploymentString) {
        this.deploymentString = deploymentString;
    }

    public BeamReference getDomain() {
        return domain;
    }

    public void setDomain(BeamReference domain) {
        this.domain = domain;
    }

    public Map<String, List<String>> getState() {
        return state;
    }

    public void setState(Map<String, List<String>> state) {
        this.state = state;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getGroupHash());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, Void cloudResource) {

    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, Void> current) throws Exception {

    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, Void> current, Set<String> changedProperties) {
        verify(cloud);
    }

    @Override
    public void delete(AzureCloud cloud) {

    }

    @Override
    public void create(AzureCloud cloud) {

    }

    @Override
    public boolean isVerifying() {
        return true;
    }

    public void verify(AzureCloud cloud) {
        System.out.println("");

        PrintWriter out = new AnsiRenderWriter(System.out, true);

        List<Deployment> deployments = new ArrayList<>();

        // Create verification load balancers and instances for autoscale groups
        // with the same hash group.
        for (AzureGroupResource autoscaleResource : getAutoscaleGroups()) {
            Deployment deployment = new Deployment();
            deployment.setAutoscaleResource(autoscaleResource);
            deployment.setHash(autoscaleResource.getGroupHash());

            setDeploymentStatus(autoscaleResource, deployment);

            associateProductionAutoscaleGroup(cloud, deployment, out);

            if (deployment.getLiveAutoscaleResource() == null) {
                String layerName = null;
                Map<String, String> tags = deployment.getAutoscaleResource().getTags();
                layerName = tags.get("beam.layer");

                out.format("The load balancer(s) are not associated with any instances, @|bold,blue skip verification for layer %s|@.", autoscaleResource.getRegion() + " " + layerName);
                out.flush();

                deployment.getAutoscaleResource().deleteVerifyingTag(cloud);

                String loadBalancerName = null;
                for (BeamReference elb : deployment.getAutoscaleResource().getLoadBalancers()) {
                    LoadBalancerResource loadBalancerResource = (LoadBalancerResource)elb.resolve();
                    loadBalancerName = loadBalancerResource.getName();
                }

                deployment.getAutoscaleResource().attachVirtualMachine(cloud, loadBalancerName);
                continue;
            }

            deployments.add(deployment);
            createVerificationLoadBalancer(cloud, deployment, out);
            createVerificationInstances(cloud, deployment, out);
        }

        BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
        while (!deployments.isEmpty()) {

            displayCurrentLoadBalancerState(cloud, deployments, out);

            out.println();
            if (deployments.get(0).getStatus() == Deployment.Status.PUSHED) {
                out.println("@|bold,magenta revert|@) Revert the new verification instances into the production load balancer and deregister old instances.");
                out.println("@|bold,magenta commit|@) Commit verification instances to production load balancer.");
            } else {
                out.println("@|bold,magenta push|@)   Push the new verification instances into the production load balancer and deregister old instances.");
                out.println("@|bold,magenta reset|@)  Delete verification instances and load balancer, then exit Beam.");
            }

            out.println("@|bold,red quit|@)   Stop the verification process for now and exit Beam.");
            out.println();
            out.print("?) ");
            out.flush();

            try {
                String pick = pickReader.readLine();

                out.println();

                if ("quit".equalsIgnoreCase(pick)) {
                    throw new BeamException(String.format(
                            "Verification instances and load balancer are still running, \"beam up\" to continue.",
                            "nope"));
                } else if ("push".equalsIgnoreCase(pick)) {
                    push(cloud, deployments, out);
                } else if ("revert".equalsIgnoreCase(pick)) {
                    revert(cloud, deployments, out);
                } else if ("commit".equalsIgnoreCase(pick)) {
                    commit(cloud, deployments, out);
                    break;
                } else if ("reset".equalsIgnoreCase(pick)) {
                    reset(cloud, deployments, out);
                    break;
                } else if ("debug".equalsIgnoreCase(pick)) {
                    debug(cloud, deployments, out);
                }

            } catch (IOException ex) {

            }
        }
    }

    private void recalculateLoadBalancerState(AzureCloud cloud, List<Deployment> deployments) {
        Map<String, List<String>> state = new TreeMap<>();

        for (Deployment deployment : deployments) {
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                List<String> g = state.get(loadBalancerResource.getRegion() + " " + loadBalancerResource.getName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getRegion() + " " + loadBalancerResource.getName(), g);
                }

                Set<String> groups = groupsInLoadBalancer(cloud, deployment, loadBalancerResource);
                g.addAll(groups);

                if (groups.contains(deployment.getAutoscaleResource().getName())) {
                    deployment.setStatus(Deployment.Status.PUSHED);
                } else {
                    deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
                }
            }

            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                List<String> g = state.get(loadBalancerResource.getRegion() + " " + loadBalancerResource.getName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getRegion() + " " + loadBalancerResource.getName(), g);
                }

                Set<String> groups = groupsInLoadBalancer(cloud, deployment, loadBalancerResource);
                g.addAll(groups);
            }
        }

        setState(state);
    }

    private Set<String> groupsInLoadBalancer(AzureCloud cloud,
                                             Deployment deployment,
                                             LoadBalancerResource loadBalancerResource) {

        Set<String> groups = new HashSet<>();

        AzureGroupResource azureGroupResource = deployment.getAutoscaleResource();
        AzureGroupResource liveGroupResource = deployment.getLiveAutoscaleResource();

        for (BeamReference reference : azureGroupResource.getLoadBalancers()) {
            if (reference.awsId() != null && reference.awsId().equals(loadBalancerResource.getName())) {
                groups.add(azureGroupResource.getName());
            }
        }

        for (BeamReference reference : liveGroupResource.getLoadBalancers()) {
            if (reference.awsId() != null && reference.awsId().equals(loadBalancerResource.getName())) {
                groups.add(liveGroupResource.getName());
            }
        }

        return groups;
    }

    private void displayCurrentLoadBalancerState(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {
        recalculateLoadBalancerState(cloud, deployments);

        out.println();
        for (String lbName : getState().keySet()) {
            if (state.get(lbName).size() == 0) {
                out.println(String.format("@|bold,blue %s|@ load balancer has no groups in it.", lbName));
            } else {
                out.println(String.format("@|bold,blue %s|@ load balancer has the following groups in it: ", lbName));

                for (String groupName : getState().get(lbName)) {
                    out.println("  -> @|green " + groupName + "|@");
                }
            }

            out.println();
        }

        out.println();
        out.println("Verification load balancers are: ");
        for (Deployment deployment : deployments) {
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                if (loadBalancerResource.getVerificationHostnames().size() > 0) {
                    for (String hostname : loadBalancerResource.getVerificationHostnames()) {
                        out.println("  -> @|green " + loadBalancerResource.getRegion() + " " + loadBalancerResource.getName() + "|@: " + hostname);
                    }
                } else {
                    for (String hostname : loadBalancerResource.getHostnames()) {
                        out.println("  -> @|green " + loadBalancerResource.getRegion() + " " + loadBalancerResource.getName() + "|@: " + hostname);
                    }
                }
            }
        }
    }

    private void associateProductionAutoscaleGroup(AzureCloud cloud, Deployment deployment, PrintWriter out) {
        AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();
        out.println();

        // Find the old auto scaling groups that are associated with the same
        // load balancers as this one.

        if (deployment.getStatus().equals(Deployment.Status.PUSHED)) {
            String productionASName = autoscaleResource.getTags().get("beam.prodASName");

            AzureGroupResource liveAutoscaleResource = new AzureGroupResource();
            liveAutoscaleResource.setRegion(autoscaleResource.getRegion());
            liveAutoscaleResource.setName(productionASName);
            liveAutoscaleResource.init(cloud, null, null);
            deployment.setLiveAutoscaleResource(liveAutoscaleResource);

        } else {
            Set<String> pendingElbNames = new HashSet<>();

            for (BeamReference lbRef : autoscaleResource.getLoadBalancers()) {
                String lbName = lbRef.awsId();
                if (deployment.getStatus().equals(Deployment.Status.INSTANCES_CREATED)) {
                    lbName = lbName.substring(0, lbName.length() - 2);
                }

                pendingElbNames.add(lbName);
            }

            for (AzureGroupResource currentGroup : getCurrentGroups()) {
                Set<String> liveLbNames = new HashSet<>();
                for (BeamReference lbRef : currentGroup.getLoadBalancers()) {
                    if (lbRef.awsId() != null) {
                        liveLbNames.add(lbRef.awsId());
                    }
                }

                if (!autoscaleResource.getName().equals(currentGroup.getName())
                        && !Collections.disjoint(pendingElbNames, liveLbNames) && !currentGroup.getVirtualMachines().isEmpty()) {

                    deployment.setLiveAutoscaleResource(currentGroup);
                }
            }
        }
    }

    private void deleteVerificationLoadBalancer(AzureCloud cloud, Deployment deployment, PrintWriter out) {
        for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
            loadBalancerResource.delete(cloud);

            ZoneResource zoneResource = getHostedZone();
            String domain = BeamRuntime.getCurrentRuntime().getSubDomain();
            if (zoneResource != null) {
                for (String hostname : loadBalancerResource.getVerificationHostnames()) {
                    if (hostname.lastIndexOf(domain) != -1) {
                        hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                    }

                    RecordSetResource record = new RecordSetResource();
                    record.setName(hostname);
                    record.setRegion("global");
                    record.setType("A");
                    record.setTTL(60);
                    record.setZone(record.newReference(zoneResource));
                    record.delete(cloud);
                }
            }
        }

        AzureGroupResource azureGroupResource = deployment.getAutoscaleResource();
        for (VirtualMachineResource virtualMachineResource : azureGroupResource.getVirtualMachines()) {
            for (NetworkInterfaceResource networkInterfaceResource : virtualMachineResource.getNetworkInterfaceResources()) {
                networkInterfaceResource.setPublicIpAllocation("None");
                networkInterfaceResource.getTags().remove("beam.verifying");
                Set<String> changes = new HashSet<>();
                changes.add("publicIpAllocation");
                networkInterfaceResource.update(cloud, networkInterfaceResource, changes);
            }
        }
    }

    private void createVerificationInstances(AzureCloud cloud, Deployment deployment, PrintWriter out) {
        if (!deployment.getStatus().equals(Deployment.Status.UNDEPLOYED)) {
            return;
        }

        AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();

        for (BeamReference name : autoscaleResource.getLoadBalancers()) {
            System.out.println("create instance: " + name.awsId());
        }

        String loadBalancerName = null;
        for (VirtualMachineResource virtualMachineResource : autoscaleResource.getVirtualMachines()) {
            for (NetworkInterfaceResource networkInterfaceResource : virtualMachineResource.getNetworkInterfaceResources()) {
                networkInterfaceResource.setPublicIpAllocation("Dynamic");
                networkInterfaceResource.getTags().put("beam.verifying", "true");
                Set<String> changes = new HashSet<>();
                changes.add("publicIpAllocation");
                networkInterfaceResource.update(cloud, networkInterfaceResource, changes);
            }
        }

        for (BeamReference elb : autoscaleResource.getLoadBalancers()) {
            LoadBalancerResource loadBalancerResource = (LoadBalancerResource)elb.resolve();
            loadBalancerName = loadBalancerResource.getName();
            ZoneResource zoneResource = getHostedZone();
            String domain = BeamRuntime.getCurrentRuntime().getSubDomain();
            if (zoneResource != null) {
                for (String hostname : loadBalancerResource.getVerificationHostnames()) {
                    if (hostname.lastIndexOf(domain) != -1) {
                        hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                    }

                    RecordSetResource record = new RecordSetResource();
                    record.getTags().put("beam.env", "network");
                    record.getTags().put("beam.layer", "loadBalancer");
                    record.setName(hostname);
                    record.setRegion("global");
                    record.setType("A");
                    record.setTTL(60);
                    record.setZone(record.newReference(zoneResource));

                    for (VirtualMachineResource virtualMachineResource : autoscaleResource.getVirtualMachines()) {
                        record.getValues().add(new RecordSetResource.ReferenceValue(record.newReference(virtualMachineResource)));
                    }

                    record.update(cloud, record, new HashSet<>());
                }
            }
        }

        deployment.getAutoscaleResource().attachVirtualMachine(cloud, loadBalancerName);
        autoscaleResource.updateVerifyingTag(cloud, "beam.verifying", "INSTANCES_CREATED");
    }

    private void createVerificationLoadBalancer(AzureCloud cloud, Deployment deployment, PrintWriter out) {
        AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();

        // Create a verification load balancer if one doesn't already exist.
        Iterator<BeamReference> iter = autoscaleResource.getLoadBalancers().iterator();
        while (iter.hasNext()) {
            BeamReference lbRef = iter.next();

            String lbName = lbRef.awsId();
            if (deployment.getStatus().equals(Deployment.Status.INSTANCES_CREATED)) {
                lbName = lbName.substring(0, lbName.length()-2);
            }

            final String liveLbName = lbName;
            final String verificationLbName = liveLbName + "-v";

            LoadBalancerResource lb = null;
            LoadBalancerResource verificationLb = null;
            for (LoadBalancerResource elb : getCurrentElbs()) {
                if (elb.getName().equals(liveLbName) && elb.getRegion().equals(autoscaleResource.getRegion())) {
                    lb = elb;
                } else if (elb.getName().equals(verificationLbName) && elb.getRegion().equals(autoscaleResource.getRegion())) {
                    verificationLb = elb;
                }
            }

            if (lb == null) {
                throw new BeamException("Unable to find live loadbalancer " + liveLbName);
            }

            if (verificationLb == null) {
                verificationLb = new LoadBalancerResource();
                verificationLb.setName(verificationLbName);
                verificationLb.setIdleTimeout(lb.getIdleTimeout());
                verificationLb.setListeners(lb.getListeners());
                verificationLb.setHostnames(lb.getHostnames());
                verificationLb.setVerificationHostnames(lb.getVerificationHostnames());
                verificationLb.setNumberOfProbes(lb.getNumberOfProbes());
                verificationLb.setProbeInterval(lb.getProbeInterval());
                verificationLb.setProbePath(lb.getProbePath());
                verificationLb.setProbePort(lb.getProbePort());
                verificationLb.setTags(lb.getTags());
                verificationLb.setRegion(lb.getRegion());
                verificationLb.setProbeProtocol(lb.getProbeProtocol());

                out.println();
                out.println("@|bold,blue Verification|@: Creating verification load balancer: @|bold,blue " + lb.getRegion() + " " + verificationLbName + "|@ ");
                out.flush();

                verificationLb.create(cloud);

                // Set verification hostname.
                ZoneResource zoneResource = getHostedZone();
                String domain = BeamRuntime.getCurrentRuntime().getSubDomain();
                if (zoneResource != null) {
                    for (String hostname : lb.getVerificationHostnames()) {
                        if (hostname.lastIndexOf(domain) != -1) {
                            hostname = hostname.substring(0, hostname.lastIndexOf(domain)-1);
                        }

                        RecordSetResource record = new RecordSetResource();
                        record.getTags().put("beam.env", "network");
                        record.getTags().put("beam.layer", "loadBalancer");
                        record.setName(hostname);
                        record.setRegion("global");
                        record.setType("A");
                        record.setTTL(60);
                        record.setZone(record.newReference(zoneResource));
                        record.create(cloud);
                    }
                }
            } else {
                verificationLb.setHostnames(lb.getHostnames());
                verificationLb.setVerificationHostnames(lb.getVerificationHostnames());
            }

            if (!deployment.getStatus().equals(Deployment.Status.PUSHED)) {
                // Remove the "live" load balancer from this autoscale group so instances don't go live
                // before verification. This load balancer will be added back during the
                // "push" command.
                iter.remove();
            }

            deployment.getLiveLoadBalancers().add(lb);
            deployment.getVerificationLoadBalancers().add(verificationLb);
        }

        if (!deployment.getStatus().equals(Deployment.Status.PUSHED)) {
            // Add the verification load balancers to this autoscale group.
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {

        }
    }

    private void reset(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {
        out.print("Executing: @|red - Deleting verification load balancers |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();
            autoscaleResource.getLoadBalancers().clear();
            autoscaleResource.attachVirtualMachine(cloud, null);
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|red - Deleting verification autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();
            for (VirtualMachineResource virtualMachineResource : autoscaleResource.getVirtualMachines()) {
                virtualMachineResource.delete(cloud);
            }
        }
    }

    private void push(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {
        for (Deployment deployment : deployments) {
            // Change the pending autoscale group's load balanacer configuration to point
            // the production load balancers.
            AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();
            autoscaleResource.getLoadBalancers().clear();
            autoscaleResource.attachVirtualMachine(cloud, null);

            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                out.println(String.format("Pushing instances for group @|bold,blue %s %s|@ into production load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getRegion(),
                        autoscaleResource.getName(),
                        loadBalancerResource.getName()));

                autoscaleResource.attachVirtualMachine(cloud, loadBalancerResource.getName());
                autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
            }

            AzureGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
            liveAutoscaleResource.getLoadBalancers().clear();
            liveAutoscaleResource.attachVirtualMachine(cloud, null);

            deployment.setStatus(Deployment.Status.PUSHED);
            autoscaleResource.updateVerifyingTag(cloud, "beam.verifying", "PUSHED");
            autoscaleResource.updateVerifyingTag(cloud, "beam.prodASName", liveAutoscaleResource.getName());
        }
    }

    private void revert(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {
        for (Deployment deployment : deployments) {
            AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();

            // Put production instances into production load balancer.
            AzureGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
            if (liveAutoscaleResource != null) {
                for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                    liveAutoscaleResource.attachVirtualMachine(cloud, loadBalancerResource.getName());
                    liveAutoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
                }
            }

            // Put verification instances into verification load balancer.
            autoscaleResource.getLoadBalancers().clear();
            autoscaleResource.attachVirtualMachine(cloud, null);

            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                out.println(String.format("Reverting instances for group @|bold,blue %s %s|@ back to verification load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getRegion(),
                        autoscaleResource.getName(),
                        loadBalancerResource.getName()));

                autoscaleResource.attachVirtualMachine(cloud, loadBalancerResource.getName());
                autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
            }

            deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
            autoscaleResource.updateVerifyingTag(cloud, "beam.verifying", "INSTANCES_CREATED");
        }
    }

    private void commit(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {

        out.print("Executing: @|red + Deleting verification load balancers |@");
        out.flush();
        for (Deployment deployment : deployments) {
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|green + Committing autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AzureGroupResource autoscaleResource = deployment.getAutoscaleResource();
            autoscaleResource.deleteVerifyingTag(cloud);
        }
    }

    private void debug(AzureCloud cloud, List<Deployment> deployments, PrintWriter out) {
        for (Deployment deployment : deployments) {
            System.out.println("     ASG: " + deployment.getAutoscaleResource().getName());

            if (deployment.getLiveAutoscaleResource() != null) {
                System.out.println("Live ASG: " + deployment.getLiveAutoscaleResource().getName());
            }
        }
    }

    private void setDeploymentStatus(AzureGroupResource autoscaleResource, Deployment deployment) {
        String status = "UNDEPLOYED";
        if (autoscaleResource.getTags().get("beam.verifying") != null) {
            status = autoscaleResource.getTags().get("beam.verifying");
        }

        if (status.equals("INSTANCES_CREATED")) {
            deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
        } else if (status.equals("PUSHED")) {
            deployment.setStatus(Deployment.Status.PUSHED);
        } else if (status.equals("UNDEPLOYED")) {
            deployment.setStatus(Deployment.Status.UNDEPLOYED);
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder builder = new StringBuilder();

        builder.append("deployment for layer(s): ");
        for (AzureGroupResource asgResource : getAutoscaleGroups()) {
            String layerName = asgResource.getTags().get("beam.layer");
            builder.append("[" + asgResource.getRegion() + " " + layerName + "]" + " ");
        }

        if (getDeploymentString() != null) {
            builder.append("\n");
            builder.append(String.format("matching [image: %s, ", getImage()));
            builder.append(getDeploymentString());
            builder.append("] ");
        }

        return builder.toString();
    }
}
