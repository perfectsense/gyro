package beam.openstack.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;
import org.fusesource.jansi.AnsiRenderWriter;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.features.PortApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.GroupInstance;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.AddNode;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.Node;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.internal.BaseNode;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.NodeApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

public class DeploymentResource extends OpenStackResource<Void> {

    private List<AutoscaleResource> autoscaleGroups;
    private String image;
    private String instanceType;
    private String groupHash;
    private String deploymentString;
    private BeamReference domain;

    private transient Map<String, List<String>> state;

    public List<AutoscaleResource> getAutoscaleGroups() {
        if (autoscaleGroups == null) {
            autoscaleGroups = new ArrayList<>();
        }

        return autoscaleGroups;
    }

    public void setAutoscaleGroups(List<AutoscaleResource> autoscaleGroups) {
        this.autoscaleGroups = autoscaleGroups;
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
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Void cloudResource) {

    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Void> current) throws Exception {
        DeploymentResource deploymentResource = (DeploymentResource) current;

        update.update(deploymentResource.getAutoscaleGroups(), getAutoscaleGroups());
    }


    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Void> current, Set<String> changedProperties) {
        verify(cloud);
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        for (AutoscaleResource autoscaleResource : getAutoscaleGroups()) {
            autoscaleResource.delete(cloud);
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {
        verify(cloud);
    }

    @Override
    public boolean isVerifying() {
        for (AutoscaleResource autoScaleResource : getAutoscaleGroups()) {
            for (String key : autoScaleResource.getMetadata().keySet()) {
                String value = autoScaleResource.getMetadata().get(key);
                if ("verifying".equals(key) && "true".equals(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void verify(OpenStackCloud cloud) {
        PrintWriter out = new AnsiRenderWriter(System.out, true);

        List<Deployment> deployments = new ArrayList<>();

        // Create verification load balancers and instances for autoscale groups
        // with the same hash group.
        for (AutoscaleResource autoscaleResource : getAutoscaleGroups()) {
            Deployment deployment = new Deployment();
            deployment.setAutoscaleResource(autoscaleResource);
            deployment.setHash(autoscaleResource.getGroupHash());
            deployments.add(deployment);

            associateProductionAutoscaleGroup(cloud, deployment, out);
            createVerificationLoadBalancer(cloud, deployment, out);
            createVerificationInstances(cloud, deployment, out);
        }

        // Wait for all verification instances to be ready.
        out.println();
        for (Deployment deployment : deployments) {
            waitForAutoscaleInstances(cloud, deployment, out);
        }

        BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {

            displayCurrentLoadBalancerState(cloud, deployments, out);

            out.println();
            if (deployments.get(0).getStatus() == Deployment.Status.PUSHED) {
                out.println("@|bold,magenta revert|@)   Revert the new verification instances into the production load balancer and put the old instances in standby.");
                out.println("@|bold,magenta commit|@)  Commit verification instances to production load balancer.");
            } else {
                out.println("@|bold,magenta push|@)   Push the new verification instances into the production load balancer and put the old instances in standby.");
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

    private void recalculateLoadBalancerState(OpenStackCloud cloud, List<Deployment> deployments) {
        Map<String, List<String>> state = new TreeMap<>();

        for (Deployment deployment : deployments) {
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                List<String> g = state.get(loadBalancerResource.getName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getName(), g);
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
                List<String> g = state.get(loadBalancerResource.getName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getName(), g);
                }

                Set<String> groups = groupsInLoadBalancer(cloud, deployment, loadBalancerResource);
                g.addAll(groups);
            }
        }

        setState(state);
    }

    private void displayCurrentLoadBalancerState(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
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
                        out.println("  -> @|green " + loadBalancerResource.getName() + "|@: " + hostname);
                    }
                } else {
                    out.println("  -> @|green " + loadBalancerResource.getName() + "|@: " + loadBalancerResource.getVirtualIp4());
                }
            }
        }
    }

    private void associateProductionAutoscaleGroup(OpenStackCloud cloud, Deployment deployment, PrintWriter out) {
        AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
        String region = autoscaleResource.getRegion();

        out.println();

        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(region);

        CloudLoadBalancersApi clbApi = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = clbApi.getLoadBalancerApi(region);

        Set<String> pendingElbNames = new HashSet<>();
        for (BeamReference lbRef : autoscaleResource.getLaunchConfig().getLoadBalancers()) {
            LoadBalancerResource loadBalancerResource = (LoadBalancerResource) lbRef.resolve();
            pendingElbNames.add(loadBalancerResource.getName());
        }

        String name = autoscaleResource.getName();
        String prefix = autoscaleResource.getGroupPrefix();
        for (GroupState groupState : groupApi.listGroupStates().toList()) {
            try {
                Group group = groupApi.get(groupState.getId());

                Set<String> liveLbNames = new HashSet<>();
                for (org.jclouds.rackspace.autoscale.v1.domain.LoadBalancer loadBalanacer : group.getLaunchConfiguration().getLoadBalancers()) {
                    LoadBalancer lb = lbApi.get(loadBalanacer.getId());
                    if (lb != null) {
                        liveLbNames.add(lb.getName());
                    }
                }

                AutoscaleResource liveAutoscaleResource = new AutoscaleResource();
                liveAutoscaleResource.setRegion(region);
                liveAutoscaleResource.init(cloud, null, group);
                if (!name.equals(group.getGroupConfiguration().getName()) &&
                        liveAutoscaleResource.getGroupPrefix().equals(prefix)) {
                    deployment.setLiveAutoscaleResource(liveAutoscaleResource);
                }
            } catch (NullPointerException npe) {
                // Skipping NPE which is most likely a jclouds parsing error.
            }
        }
    }

    private void updateServerMeta(OpenStackCloud cloud, LoadBalancerResource loadBalancerResource, AutoscaleResource autoscaleResource) {
        if (autoscaleResource == null) {
            return;
        }

        NovaApi novaApi = cloud.createApi();
        ServerApi serverApi = novaApi.getServerApi(autoscaleResource.getRegion());
        String clbKeyPrefix = "rax:autoscale:lb:CloudLoadBalancer:";
        String clbId = loadBalancerResource != null ? loadBalancerResource.getLoadBalancerId().toString() : null;

        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(autoscaleResource.getRegion());
        GroupState state = groupApi.getState(autoscaleResource.getGroupId());

        Set<String> deviceIds = new HashSet<>();
        for (GroupInstance instance : state.getGroupInstances()) {
            deviceIds.add(instance.getId());
        }

        for (String serverId : deviceIds) {
            Map<String, String> serverMeta = serverApi.getMetadata(serverId);
            String oldClbKey = null;
            for (String key : serverMeta.keySet()) {
                if (key.startsWith(clbKeyPrefix)) {
                    oldClbKey = key;
                }
            }

            if (clbId != null) {
                Map<String, String> metadata = new HashMap(serverMeta);
                String port = oldClbKey != null ? metadata.get(oldClbKey) : "[{\"port\": " + loadBalancerResource.getPort() + "}]";
                metadata.remove(oldClbKey);
                metadata.put(clbKeyPrefix + clbId, port);
                serverApi.updateMetadata(serverId, metadata);
            }

            if (oldClbKey != null) {
                serverApi.deleteMetadata(serverId, oldClbKey);
            }
        }
    }

    private void deleteVerificationLoadBalancer(OpenStackCloud cloud, Deployment deployment, PrintWriter out) {
        for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
            loadBalancerResource.delete(cloud);
        }
    }

    private void createVerificationLoadBalancer(OpenStackCloud cloud, Deployment deployment, PrintWriter out) {
        AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
        String region = autoscaleResource.getRegion();

        // Create a verification load balancer if one doesn't already exist.
        Iterator<BeamReference> iter = autoscaleResource.getLaunchConfig().getLoadBalancers().iterator();
        while (iter.hasNext()) {
            BeamReference lbRef = iter.next();
            LoadBalancerResource lb = (LoadBalancerResource) lbRef.resolve();
            final String liveLbName = lb.getName();
            final String verificationLbName = liveLbName + "-v";

            CloudLoadBalancersApi clbApi = cloud.createCloudLoadBalancersApi();
            LoadBalancerApi lbApi = clbApi.getLoadBalancerApi(region);

            Optional<LoadBalancer> opt = lbApi.list().concat().
                    firstMatch(new Predicate<LoadBalancer>() {
                        @Override
                        public boolean apply(@Nullable LoadBalancer loadBalancers) {
                            if (loadBalancers.getName().equals(verificationLbName)) {
                                return true;
                            }

                            return false;
                        }

                        ;
                    });

            LoadBalancerResource verificationLb = new LoadBalancerResource();
            if (opt.isPresent()) {
                verificationLb.init(cloud, null, opt.get());
            } else {
                out.println();
                out.print("@|bold,blue Verification|@: Creating verification load balancer: @|bold,blue " + verificationLbName + "|@ ");
                out.flush();

                verificationLb.init(cloud, null, lb.getLoadBalancer());
                verificationLb.setName(verificationLbName);
                verificationLb.setVerificationHostnames(lb.getVerificationHostnames());
                verificationLb.create(cloud);
            }

            // Remove the "live" load balancer from this autoscale group so instances don't go live
            // before verification. This load balancer will be added back during the
            // "push" command.
            iter.remove();

            deployment.getLiveLoadBalancers().add(lb);
            deployment.getVerificationLoadBalancers().add(verificationLb);

            // Set verification hostname.
            DomainResource domainResource = (DomainResource) getDomain().resolve();
            for (String hostname : lb.getVerificationHostnames()) {
                if (!hostname.endsWith(".")) {
                    hostname = hostname + "." + domain;
                } else {
                    hostname = hostname.substring(0, hostname.length() - 1);
                }

                // Check to see if record already exists.
                RecordDetail oldRecordDetail = null;
                CloudDNSApi dnsApi = cloud.createCloudDnsApi();
                RecordApi recordApi = dnsApi.getRecordApi(domainResource.getDomainId());
                for (RecordDetail recordDetail : recordApi.list().concat()) {
                    String type = recordDetail.getType();

                    if (!"NS".equals(type) && !"SOA".equals(type)) {
                        if (recordDetail.getName().equals(hostname)) {
                            oldRecordDetail = recordDetail;
                            break;
                        }
                    }
                }

                DomainRecordResource recordResource = new DomainRecordResource();
                if (oldRecordDetail != null) {
                    recordResource.init(cloud, null, oldRecordDetail);
                }

                recordResource.setDomain(recordResource.newReference(domainResource));
                domainResource.getRecords().add(recordResource);
                recordResource.setName(hostname);
                recordResource.setTTL(300);
                recordResource.setType("A");

                DomainRecordResource.ReferenceValue value =
                        new DomainRecordResource.ReferenceValue(recordResource.newReference(verificationLb));
                value.setType("A");

                recordResource.setValue(value);

                if (oldRecordDetail != null) {
                    recordResource.update(cloud, null, null);
                } else {
                    recordResource.create(cloud);
                }
            }
        }

        // Add the verification load balancers to this autoscale group.
        for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
            autoscaleResource.getLaunchConfig().getLoadBalancers().add(autoscaleResource.newReference(loadBalancerResource));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {

        }

        deployment.setStatus(Deployment.Status.LOADBALANCER_CREATED);
    }

    private void createVerificationInstances(OpenStackCloud cloud, Deployment deployment, PrintWriter out) {
        AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
        String region = autoscaleResource.getRegion();

        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(region);

        Map<String, String> verifyingMetadata = autoscaleResource.getMetadata();
        verifyingMetadata.put("verifying", "true");

        if (autoscaleResource.getGroupId() == null) {
            autoscaleResource.setMetadata(verifyingMetadata);
            autoscaleResource.create(cloud);
        } else {
            GroupConfiguration groupConfig = GroupConfiguration.builder()
                    .maxEntities(autoscaleResource.getMaxEntities())
                    .minEntities(autoscaleResource.getMinEntities())
                    .cooldown(autoscaleResource.getCoolDown())
                    .metadata(verifyingMetadata)
                    .name(autoscaleResource.getName())
                    .build();

            groupApi.updateGroupConfiguration(autoscaleResource.getGroupId(), groupConfig);
            groupApi.updateLaunchConfiguration(autoscaleResource.getGroupId(), autoscaleResource.getLaunchConfig().buildLaunchConfiguration());
        }
    }

    private void reset(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
        out.print("Executing: @|red + Deleting verification load balancers |@");
        for (Deployment deployment : deployments) {
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|red + Deleting verification autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
            String region = autoscaleResource.getRegion();

            AutoscaleApi api = cloud.createAutoscaleApi();
            GroupApi groupApi = api.getGroupApi(region);

            Map<String, String> verifyingMetadata = autoscaleResource.getMetadata();
            verifyingMetadata.put("verifying", "true");

            GroupConfiguration groupConfig = GroupConfiguration.builder()
                    .maxEntities(0)
                    .minEntities(0)
                    .cooldown(autoscaleResource.getCoolDown())
                    .metadata(verifyingMetadata)
                    .name(autoscaleResource.getName())
                    .build();

            groupApi.updateGroupConfiguration(autoscaleResource.getGroupId(), groupConfig);
            waitForAutoscaleInstances(cloud, deployment, out);

            groupApi.delete(autoscaleResource.getGroupId());
        }
    }

    private void push(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
        AutoscaleApi api = cloud.createAutoscaleApi();

        for (Deployment deployment : deployments) {
            // Change the pending autoscale group's load balanacer configuration to point
            // the production load balancers.
            AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
            String region = autoscaleResource.getRegion();

            autoscaleResource.getLaunchConfig().getLoadBalancers().clear();
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                autoscaleResource.getLaunchConfig().getLoadBalancers().add(newReference(loadBalancerResource));
            }

            GroupApi groupApi = api.getGroupApi(region);
            groupApi.updateLaunchConfiguration(autoscaleResource.getGroupId(), autoscaleResource.getLaunchConfig().buildLaunchConfiguration());

            // Change the production autoscale group's load balancer configuration to
            // be blank so new instances do not go into the production load balancer.
            AutoscaleResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
            if (liveAutoscaleResource != null) {
                liveAutoscaleResource.getLaunchConfig().getLoadBalancers().clear();

                groupApi.updateLaunchConfiguration(liveAutoscaleResource.getGroupId(), liveAutoscaleResource.getLaunchConfig().buildLaunchConfiguration());
            }

            // Move instances from verification to producation.
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);

                out.println(String.format("Pushing instances for group @|bold,blue %s|@ into production load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getName(),
                        liveLb.getName()));

                moveNodes(cloud, verificationLb, liveLb, out);
                updateServerMeta(cloud, liveLb, autoscaleResource);
                updateServerMeta(cloud, null, liveAutoscaleResource);
            }

            deployment.setStatus(Deployment.Status.PUSHED);
        }
    }

    private void revert(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
        AutoscaleApi api = cloud.createAutoscaleApi();

        for (Deployment deployment : deployments) {
            AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
            String region = autoscaleResource.getRegion();

            GroupApi groupApi = api.getGroupApi(region);

            // Put production instances into production load balancer.
            AutoscaleResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
            if (liveAutoscaleResource != null) {
                liveAutoscaleResource.getLaunchConfig().getLoadBalancers().clear();
                for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                    liveAutoscaleResource.getLaunchConfig().getLoadBalancers().add(newReference(loadBalancerResource));
                }

                groupApi.updateLaunchConfiguration(liveAutoscaleResource.getGroupId(), liveAutoscaleResource.getLaunchConfig().buildLaunchConfiguration());
            }

            // Put verification instances into verification load balancer.
            autoscaleResource.getLaunchConfig().getLoadBalancers().clear();
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                autoscaleResource.getLaunchConfig().getLoadBalancers().add(newReference(loadBalancerResource));
            }

            groupApi.updateLaunchConfiguration(autoscaleResource.getGroupId(), autoscaleResource.getLaunchConfig().buildLaunchConfiguration());

            // Move instances from production to verification.
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);

                out.println(String.format("Reverting instances for group @|bold,blue %s|@ back to verification load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getName(),
                        verificationLb.getName()));

                addNodes(cloud, deployment.getLiveAutoscaleResource(), liveLb, out);
                removeNodes(cloud, deployment.getAutoscaleResource(), liveLb, out);
                updateServerMeta(cloud, verificationLb, autoscaleResource);
                updateServerMeta(cloud, liveLb, liveAutoscaleResource);
            }

            deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
        }
    }

    private void commit(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
        AutoscaleApi api = cloud.createAutoscaleApi();

        out.print("Executing: @|red + Deleting verification load balancers |@");
        for (Deployment deployment : deployments) {
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|green + Committing autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
            String region = autoscaleResource.getRegion();

            GroupApi groupApi = api.getGroupApi(region);

            Map<String, String> verifyingMetadata = autoscaleResource.getMetadata();
            verifyingMetadata.put("verifying", "false");

            GroupConfiguration groupConfig = GroupConfiguration.builder()
                    .maxEntities(autoscaleResource.getMaxEntities())
                    .minEntities(autoscaleResource.getMinEntities())
                    .cooldown(autoscaleResource.getCoolDown())
                    .metadata(verifyingMetadata)
                    .name(autoscaleResource.getName())
                    .build();

            groupApi.updateGroupConfiguration(autoscaleResource.getGroupId(), groupConfig);
        }
    }

    private Set<String> groupsInLoadBalancer(OpenStackCloud cloud,
                                             Deployment deployment,
                                             LoadBalancerResource loadBalancerResource) {

        Set<String> groups = new HashSet<>();

        CloudLoadBalancersApi lbApi = cloud.createCloudLoadBalancersApi();
        NodeApi nodeApi = lbApi.getNodeApi(loadBalancerResource.getRegion(), loadBalancerResource.getLoadBalancerId());

        NeutronApi neutronApi = cloud.createNeutronApi();
        PortApi portApi = neutronApi.getPortApi(loadBalancerResource.getRegion());

        // Map the instance IPs to a device id.
        Map<String, String> portMap = new HashMap<>();
        for (Port port : portApi.list().concat()) {
            for (IP ip : port.getFixedIps()) {
                portMap.put(ip.getIpAddress(), port.getDeviceId());
            }
        }

        // Find device id in known autoscale groups.
        for (Node node : nodeApi.list().concat()) {
            String deviceId = portMap.get(node.getAddress());
            if (deviceId != null) {
                for (Server server : deployment.getAutoscaleResource().getServers(cloud)) {
                    if (server.getId().equals(deviceId)) {
                        groups.add(deployment.getAutoscaleResource().getName());
                    }
                }

                if (deployment.getLiveAutoscaleResource() != null) {
                    for (Server server : deployment.getLiveAutoscaleResource().getServers(cloud)) {
                        if (server.getId().equals(deviceId)) {
                            groups.add(deployment.getLiveAutoscaleResource().getName());
                        }
                    }
                }
            }
        }

        return groups;
    }

    private void addNodes(OpenStackCloud cloud, AutoscaleResource autoscaleResource, LoadBalancerResource to, PrintWriter out) {
        CloudLoadBalancersApi lbApi = cloud.createCloudLoadBalancersApi();
        NodeApi nodeApi = lbApi.getNodeApi(to.getRegion(), to.getLoadBalancerId());

        List<AddNode> nodes = new ArrayList<>();
        for (Server server : autoscaleResource.getServers(cloud)) {
            AddNode n = new AddNode.Builder()
                    .port(to.getPort())
                    .address(server.getAccessIPv4())
                    .type(BaseNode.Type.PRIMARY)
                    .condition(BaseNode.Condition.ENABLED)
                    .build();

            nodes.add(n);
        }

        nodeApi.add(nodes);

        waitForActiveLoadBalancer(cloud, to, out);
    }

    private void removeNodes(OpenStackCloud cloud, AutoscaleResource autoscaleResource, LoadBalancerResource from, PrintWriter out) {
        CloudLoadBalancersApi lbApi = cloud.createCloudLoadBalancersApi();
        NodeApi nodeApi = lbApi.getNodeApi(from.getRegion(), from.getLoadBalancerId());

        Set<String> ips = new HashSet<>();
        for (Server server : autoscaleResource.getServers(cloud)) {
            Multimap<String, Address> addresses = server.getAddresses();
            for (String networkName : addresses.keySet()) {
                if ("private".equals(networkName)) {
                    for (Object o : addresses.get(networkName)) {
                        Address address = (Address) o;
                        if (address.getVersion() == 4) {
                            ips.add(address.getAddr());
                            break;
                        }
                    }
                }
            }
        }

        List<Integer> nodeIds = new ArrayList<>();
        for (Node node : nodeApi.list().concat()) {
            if (ips.contains(node.getAddress())) {
                nodeIds.add(node.getId());
            }
        }

        if (!nodeIds.isEmpty()) {
            nodeApi.remove(nodeIds);
        }
    }

    private void moveNodes(OpenStackCloud cloud, LoadBalancerResource from, LoadBalancerResource to, PrintWriter out) {
        CloudLoadBalancersApi lbApi = cloud.createCloudLoadBalancersApi();

        NodeApi fromNodeApi = lbApi.getNodeApi(from.getRegion(), from.getLoadBalancerId());
        NodeApi toNodeApi = lbApi.getNodeApi(to.getRegion(), to.getLoadBalancerId());

        List<Integer> nodeIds = new ArrayList<>();
        for (Node node : toNodeApi.list().concat()) {
            nodeIds.add(node.getId());
        }

        List<AddNode> nodes = new ArrayList<>();
        for (Node node : fromNodeApi.list().concat()) {
            AddNode n = new AddNode.Builder()
                    .port(node.getPort())
                    .address(node.getAddress())
                    .type(node.getType())
                    .condition(node.getCondition())
                    .build();

            nodes.add(n);
        }

        toNodeApi.add(nodes);

        waitForActiveLoadBalancer(cloud, to, out);

        if (!nodeIds.isEmpty()) {
            toNodeApi.remove(nodeIds);
        }
    }

    private void waitForActiveLoadBalancer(OpenStackCloud cloud, LoadBalancerResource loadBalancerResource, PrintWriter out) {
        CloudLoadBalancersApi clbApi = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = clbApi.getLoadBalancerApi(loadBalancerResource.getRegion());

        LoadBalancer lb = lbApi.get(loadBalancerResource.getLoadBalancerId());
        while (lb.getStatus() != LoadBalancer.Status.ACTIVE) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {

            }

            lb = lbApi.get(loadBalancerResource.getLoadBalancerId());
        }
    }

    private void waitForAutoscaleInstances(OpenStackCloud cloud, Deployment deployment, PrintWriter out) {
        AutoscaleResource autoscaleResource = deployment.getAutoscaleResource();
        String region = autoscaleResource.getRegion();

        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(region);
        GroupState state = groupApi.getState(autoscaleResource.getGroupId());

        if (state.getActiveCapacity() != state.getDesiredCapacity()) {
            out.println(String.format("@|bold,blue Verification:|@ Launching instances for group %s", autoscaleResource.getName()));
        }

        int waited = 0;
        WAIT: while (state.getActiveCapacity() != state.getDesiredCapacity()) {
            String message = "launch";
            if (state.getPendingCapacity() == 0) {
                message = "terminate";
            }

            if (++waited >= 6) {
                out.print("Keep waiting? (Y/n) ");
                out.flush();

                waited = 0;

                BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                try {
                    if ("n".equalsIgnoreCase(confirmReader.readLine())) {
                        return;
                    }

                    continue WAIT;
                } catch (IOException error) {
                    throw Throwables.propagate(error);
                }
            }

            out.println(String.format(
                    "@|bold,blue Verification:|@ Waiting for @|bold,blue %d|@ instance(s) to %s in %s",
                    state.getPendingCapacity(),
                    message,
                    autoscaleResource.getName()
            ));

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {

            }

            state = groupApi.getState(autoscaleResource.getGroupId());
        }
    }

    private void debug(OpenStackCloud cloud, List<Deployment> deployments, PrintWriter out) {
        for (Deployment deployment : deployments) {
            System.out.println("     ASG: " + deployment.getAutoscaleResource().getName());

            if (deployment.getLiveAutoscaleResource() != null) {
                System.out.println("Live ASG: " + deployment.getLiveAutoscaleResource().getName());
            }
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder builder = new StringBuilder();

        if (getDeploymentString() != null) {
            builder.append(String.format("deployment for layer(s) matching [region: %s, image: %s, instanceType: %s, ",
                    getRegion(),
                    getImage(),
                    getInstanceType()
            ));

            builder.append(getDeploymentString());
            builder.append("] ");

        } else {
            builder.append("deployment for layer(s) ...");
        }

        return builder.toString();
    }

}