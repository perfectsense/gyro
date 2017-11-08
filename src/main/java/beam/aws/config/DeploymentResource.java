package beam.aws.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import org.fusesource.jansi.AnsiRenderWriter;

public class DeploymentResource extends AWSResource<Void> {

    private List<AutoScalingGroupResource> autoscaleGroups;
    private String image;
    private String instanceType;
    private String groupHash;
    private String deploymentString;
    private BeamReference domain;
    private List<AutoScalingGroupResource> nonElbGroups;

    private transient Map<String, List<String>> state;

    private static final Set<String> TERMINATING_LIFECYCLE_STATES = ImmutableSet.of(
            LifecycleState.Terminated.toString(),
            LifecycleState.Terminating.toString(),
            LifecycleState.TerminatingProceed.toString(),
            LifecycleState.TerminatingWait.toString());

    public List<AutoScalingGroupResource> getAutoscaleGroups() {
        if (autoscaleGroups == null) {
            autoscaleGroups = new ArrayList<>();
        }

        return autoscaleGroups;
    }

    public void setAutoscaleGroups(List<AutoScalingGroupResource> autoscaleGroups) {
        this.autoscaleGroups = autoscaleGroups;
    }

    public List<AutoScalingGroupResource> getNonElbGroups() {
        if (nonElbGroups == null) {
            nonElbGroups = new ArrayList<>();
        }

        return nonElbGroups;
    }

    public void setNonElbGroups(List<AutoScalingGroupResource> nonElbGroups) {
        this.nonElbGroups = nonElbGroups;
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
    public void init(AWSCloud cloud, BeamResourceFilter filter, Void cloudResource) {

    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, Void> current) throws Exception {

    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Void> current, Set<String> changedProperties) {
        verify(cloud);
    }

    @Override
    public void delete(AWSCloud cloud) {

    }

    @Override
    public void create(AWSCloud cloud) {

    }

    @Override
    public boolean isVerifying() {
        return true;
    }

    public void verify(AWSCloud cloud) {
        System.out.println("");

        PrintWriter out = new AnsiRenderWriter(System.out, true);

        List<Deployment> deployments = new ArrayList<>();

        // Create verification load balancers and instances for autoscale groups
        // with the same hash group.
        for (AutoScalingGroupResource autoscaleResource : getAutoscaleGroups()) {
            Deployment deployment = new Deployment();
            deployment.setAutoscaleResource(autoscaleResource);
            deployment.setHash(autoscaleResource.getGroupHash());

            setDeploymentStatus(autoscaleResource, deployment);

            associateProductionAutoscaleGroup(cloud, deployment, out);

            if (deployment.getLiveAutoscaleResource() == null) {
                AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
                asClient.setRegion(autoscaleResource.getRegion());
                AutoScalingGroupResource.updateMinSize(asClient, autoscaleResource.getAutoScalingGroupName(), autoscaleResource.getMinSize());
                getNonElbGroups().add(autoscaleResource);
                continue;
            }

            deployments.add(deployment);
            createVerificationLoadBalancer(cloud, deployment, out);
            createVerificationInstances(cloud, deployment, out);
        }

        if (deployments.isEmpty()) {
            for (AutoScalingGroupResource nonElbGroup : getNonElbGroups()) {
                String layerName = null;
                for (AutoScalingGroupTagResource tag : nonElbGroup.getTags()) {
                    if ("beam.layer".equals(tag.getKey())) {
                        layerName = tag.getValue();
                    }
                }

                out.format("The load balancer(s) are not associated with any instances, @|bold,blue skip verification for layer %s|@.", nonElbGroup.getRegion().getName() + " " + layerName);
                out.flush();
                AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
                asClient.setRegion(nonElbGroup.getRegion());
                AutoScalingGroupResource.deleteVerifyingTag(asClient, nonElbGroup.getAutoScalingGroupName());
            }
        }

        matchLiveInstanceSize(cloud, deployments, out);

        // Wait for all verification instances to be ready.
        out.println();
        for (Deployment deployment : deployments) {
            waitForAutoscaleInstances(cloud, deployment, out);
        }

        Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap = new HashMap<>();

        // Wait for all instances in verification load balancer to be in service
        for (Deployment deployment : deployments) {
            if (!deployment.getStatus().equals(Deployment.Status.PUSHED)) {
                for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                    addNodes(cloud, deployment.getAutoscaleResource(), null, verificationLb, out, instanceMap);
                }
            }
        }

        waitForAllLoadBalancerInstances(cloud, instanceMap);

        BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
        while (!deployments.isEmpty()) {

            displayCurrentLoadBalancerState(cloud, deployments, out);
            displayNonElbGroupHost(cloud, out);

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
                    cloud.getProvider().refresh();
                    push(cloud, deployments, out);
                } else if ("revert".equalsIgnoreCase(pick)) {
                    cloud.getProvider().refresh();
                    revert(cloud, deployments, out);
                } else if ("commit".equalsIgnoreCase(pick)) {
                    cloud.getProvider().refresh();
                    commit(cloud, deployments, out);
                    break;
                } else if ("reset".equalsIgnoreCase(pick)) {
                    cloud.getProvider().refresh();
                    reset(cloud, deployments, out);
                    break;
                } else if ("debug".equalsIgnoreCase(pick)) {
                    debug(cloud, deployments, out);
                }

            } catch (IOException ex) {

            }
        }
    }

    private void recalculateLoadBalancerState(AWSCloud cloud, List<Deployment> deployments) {
        Map<String, List<String>> state = new TreeMap<>();

        for (Deployment deployment : deployments) {
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                List<String> g = state.get(loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName(), g);
                }

                Set<String> groups = groupsInLoadBalancer(cloud, deployment, loadBalancerResource);
                g.addAll(groups);

                if (groups.contains(deployment.getAutoscaleResource().getAutoScalingGroupName())) {
                    deployment.setStatus(Deployment.Status.PUSHED);
                } else {
                    deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
                }
            }

            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                List<String> g = state.get(loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName());
                if (g == null) {
                    g = new ArrayList<>();
                    state.put(loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName(), g);
                }

                Set<String> groups = groupsInLoadBalancer(cloud, deployment, loadBalancerResource);
                g.addAll(groups);
            }
        }

        setState(state);
    }

    private void displayNonElbGroupHost(AWSCloud cloud, PrintWriter out) {
        if (!getNonElbGroups().isEmpty()) {
            out.println();
            out.println("Verification autoscale instances are: ");
            Map<String, String> tags = new HashMap<>();
            for (AutoScalingGroupResource nonElbGroup : getNonElbGroups()) {
                for (AutoScalingGroupTagResource tagResource : nonElbGroup.getTags()) {
                    tags.put(tagResource.getKey(), tagResource.getValue());
                }

                String groupName = String.format("%s %s %s %s v%s", nonElbGroup.getRegion(), tags.get("beam.project"), tags.get("beam.layer"), tags.get("beam.env"), tags.get("beam.serial"));
                DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();
                dasgRequest.setAutoScalingGroupNames(Arrays.asList(nonElbGroup.getAutoScalingGroupName()));

                AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
                asClient.setRegion(nonElbGroup.getRegion());

                AmazonEC2Client ec2Client = createClient(AmazonEC2Client.class, cloud.getProvider());
                ec2Client.setRegion(nonElbGroup.getRegion());
                Set<String> instanceIds = new HashSet<>();

                for (AutoScalingGroup asg : asClient.
                        describeAutoScalingGroups(dasgRequest).
                        getAutoScalingGroups()) {

                    for (Instance i : asg.getInstances()) {
                        instanceIds.add(i.getInstanceId());
                    }
                }

                DescribeInstancesRequest diRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
                for (Reservation reservation : ec2Client.describeInstances(diRequest).getReservations()) {
                    for (com.amazonaws.services.ec2.model.Instance ec2Instance : reservation.getInstances()) {
                        out.println("  -> @|green " + groupName + "|@: " + ec2Instance.getPrivateDnsName());
                    }
                }
            }

            out.println();
        }
    }

    private void displayCurrentLoadBalancerState(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
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
                        out.println("  -> @|green " + loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName() + "|@: " + hostname);
                    }
                } else {
                    out.println("  -> @|green " + loadBalancerResource.getRegion().getName() + " " + loadBalancerResource.getLoadBalancerName() + "|@: " + loadBalancerResource.getDnsName());
                }
            }
        }
    }

    private void associateProductionAutoscaleGroup(AWSCloud cloud, Deployment deployment, PrintWriter out) {
        AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
        out.println();

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(autoscaleResource.getRegion());

        // Find the old auto scaling groups that are associated with the same
        // load balancers as this one.

        if (deployment.getStatus().equals(Deployment.Status.PUSHED)) {
            String productionASName = "";
            for (AutoScalingGroupTagResource tag : autoscaleResource.getTags()) {
                if ("beam.prodASName".equals(tag.getKey())) {
                    productionASName = tag.getValue();
                }
            }

            for (AutoScalingGroup asg : asClient.describeAutoScalingGroups().getAutoScalingGroups()) {
                if (productionASName.equals(asg.getAutoScalingGroupName())) {
                    AutoScalingGroupResource liveAutoscaleResource = new AutoScalingGroupResource();
                    liveAutoscaleResource.setRegion(autoscaleResource.getRegion());
                    liveAutoscaleResource.init(cloud, null, asg);

                    deployment.setLiveAutoscaleResource(liveAutoscaleResource);
                }
            }
        } else {
            Set<String> pendingElbNames = new HashSet<>();

            for (BeamReference lbRef : autoscaleResource.getLoadBalancers()) {
                String lbName = lbRef.awsId();
                if (deployment.getStatus().equals(Deployment.Status.INSTANCES_CREATED)) {
                    lbName = lbName.substring(0, lbName.length() - 2);
                }

                pendingElbNames.add(lbName);
            }

            for (AutoScalingGroup asg : asClient.describeAutoScalingGroups().getAutoScalingGroups()) {
                Set<String> liveLbNames = new HashSet<>();
                liveLbNames.addAll(asg.getLoadBalancerNames());

                if (!autoscaleResource.getAutoScalingGroupName().equals(asg.getAutoScalingGroupName())
                        && !Collections.disjoint(pendingElbNames, liveLbNames) && !asg.getInstances().isEmpty()) {

                    AutoScalingGroupResource liveAutoscaleResource = new AutoScalingGroupResource();
                    liveAutoscaleResource.setRegion(autoscaleResource.getRegion());
                    liveAutoscaleResource.init(cloud, null, asg);

                    deployment.setLiveAutoscaleResource(liveAutoscaleResource);
                }
            }
        }
    }

    private void deleteVerificationLoadBalancer(AWSCloud cloud, Deployment deployment, PrintWriter out) {
        for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
            loadBalancerResource.delete(cloud);
        }
    }

    private void createVerificationLoadBalancer(AWSCloud cloud, Deployment deployment, PrintWriter out) {
        AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

        AmazonElasticLoadBalancingClient lbClient = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        lbClient.setRegion(autoscaleResource.getRegion());

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

            com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest verifyRequest = new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest();
            verifyRequest.setLoadBalancerNames(Arrays.asList(verificationLbName));

            com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest liveRequest = new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest();
            liveRequest.setLoadBalancerNames(Arrays.asList(liveLbName));

            LoadBalancerDescription verificationLbDescription = null;
            LoadBalancerDescription liveLbDescription = null;

            for (LoadBalancerDescription liveLb : lbClient.
                    describeLoadBalancers(liveRequest).
                    getLoadBalancerDescriptions()) {

                liveLbDescription = liveLb;

                break;
            }

            LoadBalancerResource lb = (LoadBalancerResource) lbRef.resolve();
            if (lb == null) {
                lb = new LoadBalancerResource();
                lb.init(cloud, null, liveLbDescription);
            }

            try {
                for (LoadBalancerDescription existingLb : lbClient.
                        describeLoadBalancers(verifyRequest).
                        getLoadBalancerDescriptions()) {

                    verificationLbDescription = existingLb;
                    break;
                }

            } catch (LoadBalancerNotFoundException error) {
                // Verification load balancer not created yet, so do so
                // below.
            }

            LoadBalancerResource verificationLb = new LoadBalancerResource();
            if (verificationLbDescription != null) {
                verificationLb.init(cloud, null, verificationLbDescription);
                verificationLb.setVerificationHostnames(lb.getVerificationHostnames());
            } else {
                out.println();
                out.println("@|bold,blue Verification|@: Creating verification load balancer: @|bold,blue " + lb.getRegion().getName() + " " + verificationLbName + "|@ ");
                out.flush();

                verificationLb.init(cloud, null, liveLbDescription);
                verificationLb.getHealthCheck().setLoadBalancer(newReference(verificationLb));

                verificationLb.setLoadBalancerName(verificationLbName);
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
            HostedZoneResource hzResource = null;
            if (autoscaleResource.getHostedZone() != null) {
                hzResource = (HostedZoneResource) autoscaleResource.getHostedZone().resolve();
            }
            if (hzResource != null) {
                for (String hostname : lb.getVerificationHostnames()) {

                    if (!hostname.endsWith(".")) {
                        hostname = hostname + ".";
                    }

                    // Find old resource set.
                    HostedZoneRRSetResource oldRecord = null;
                    for (HostedZoneRRSetResource set : autoscaleResource.findCurrentRecords(cloud)) {
                        if (set.getName().equals(hostname) && set.getType().equals("CNAME")) {
                            oldRecord = set;
                            break;
                        }
                    }

                    HostedZoneRRSetResource rrSetResource = new HostedZoneRRSetResource();

                    if (!hostname.endsWith(".")) {
                        hostname = hostname + ".";
                    }

                    hzResource.getResourceRecordSets().add(rrSetResource);
                    rrSetResource.setHostedZone(autoscaleResource.getHostedZone());
                    rrSetResource.setName(hostname);
                    rrSetResource.setTtl(60L);
                    rrSetResource.setType("CNAME");
                    rrSetResource.getValues().add(new HostedZoneRRSetResource.StringValue(verificationLb.getDnsName()));

                    if (oldRecord != null) {
                        rrSetResource.update(cloud, oldRecord, null);
                    } else {
                        rrSetResource.create(cloud);
                    }
                }
            }
        }

        // Add the verification load balancers to this autoscale group.
        for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
            autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {

        }
    }

    private void createVerificationInstances(AWSCloud cloud, Deployment deployment, PrintWriter out) {
        if (!deployment.getStatus().equals(Deployment.Status.UNDEPLOYED)) {
            return;
        }

        AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

        for (BeamReference name : autoscaleResource.getLoadBalancers()) {
            System.out.println("create instance: " + name.awsId());
        }

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(autoscaleResource.getRegion());

        detachLoadBalancer(asClient, autoscaleResource, deployment, "live");
        attachLoadBalancer(asClient, autoscaleResource, deployment, "verify");

        AutoScalingGroupResource.updateMinSize(asClient, autoscaleResource.getAutoScalingGroupName(), autoscaleResource.getMinSize());
        AutoScalingGroupResource.updateVerifyingTag(asClient, autoscaleResource.getAutoScalingGroupName(), "beam.verifying", "INSTANCES_CREATED");
    }

    private void reset(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        out.print("Executing: @|red + Deleting verification load balancers |@");
        for (Deployment deployment : deployments) {
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|red + Deleting verification autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

            AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
            asClient.setRegion(autoscaleResource.getRegion());
            AutoScalingGroupResource.updateMinSize(asClient, autoscaleResource.getAutoScalingGroupName(), 0);
            AutoScalingGroupResource.updateMaxSize(asClient, autoscaleResource.getAutoScalingGroupName(), 0);

            waitForAutoscaleInstances(cloud, deployment, out);

            autoscaleResource.delete(cloud);
        }

        for (AutoScalingGroupResource nonElbGroup : getNonElbGroups()) {
            AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
            asClient.setRegion(nonElbGroup.getRegion());
            AutoScalingGroupResource.updateMinSize(asClient, nonElbGroup.getAutoScalingGroupName(), 0);
            AutoScalingGroupResource.updateMaxSize(asClient, nonElbGroup.getAutoScalingGroupName(), 0);
            nonElbGroup.delete(cloud);
        }

        out.println("OK");
        out.print("Executing: @|red + Deleting verification autoscale group launch configuration |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
            LaunchConfigurationResource launchConfiguration = (LaunchConfigurationResource) autoscaleResource.getLaunchConfiguration().resolve();
            launchConfiguration.delete(cloud);
        }

        for (AutoScalingGroupResource nonElbGroup : getNonElbGroups()) {
            LaunchConfigurationResource launchConfiguration = (LaunchConfigurationResource) nonElbGroup.getLaunchConfiguration().resolve();
            launchConfiguration.delete(cloud);
        }
    }

    private void detachLoadBalancer(AmazonAutoScalingClient asClient, AutoScalingGroupResource autoscaleResource, Deployment deployment, String type) {
        DetachLoadBalancersRequest dlbRequest = new DetachLoadBalancersRequest();
        dlbRequest.setAutoScalingGroupName(autoscaleResource.getAutoScalingGroupName());

        List<String> elbNames = new ArrayList<>();
        List<String> allElbNames = new ArrayList<>();

        for (AutoScalingGroup asg : asClient.describeAutoScalingGroups().getAutoScalingGroups()) {
            if (asg.getAutoScalingGroupName().equals(autoscaleResource.getAutoScalingGroupName())) {
                allElbNames = asg.getLoadBalancerNames();
            }
        }

        if (type.equals("verify")) {
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                if (allElbNames.contains(loadBalancerResource.getLoadBalancerName())) {
                    elbNames.add(loadBalancerResource.getLoadBalancerName());
                }
            }
        } else if (type.equals("live")) {
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                if (allElbNames.contains(loadBalancerResource.getLoadBalancerName())) {
                    elbNames.add(loadBalancerResource.getLoadBalancerName());
                }
            }
        }

        dlbRequest.setLoadBalancerNames(elbNames);
        asClient.detachLoadBalancers(dlbRequest);
    }

    private void attachLoadBalancer(AmazonAutoScalingClient asClient, AutoScalingGroupResource autoscaleResource, Deployment deployment, String type) {
        AttachLoadBalancersRequest albRequest = new AttachLoadBalancersRequest();
        albRequest.setAutoScalingGroupName(autoscaleResource.getAutoScalingGroupName());

        List<String> elbNames = new ArrayList<>();

        if (type.equals("verify")) {
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                elbNames.add(loadBalancerResource.getLoadBalancerName());
            }
        } else if (type.equals("live")) {
            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                elbNames.add(loadBalancerResource.getLoadBalancerName());
            }
        }

        albRequest.setLoadBalancerNames(elbNames);
        asClient.attachLoadBalancers(albRequest);
    }

    private boolean matchLiveInstanceSize(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        boolean isMatch = true;

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
            AutoScalingGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();

            asClient.setRegion(autoscaleResource.getRegion());

            DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();
            dasgRequest.setAutoScalingGroupNames(Arrays.asList(liveAutoscaleResource.getAutoScalingGroupName()));

            int liveInstancesSize = 0;
            for (AutoScalingGroup asg : asClient.
                    describeAutoScalingGroups(dasgRequest).
                    getAutoScalingGroups()) {
                liveInstancesSize = asg.getInstances().size();
            }

            if (liveInstancesSize == 0) {
                return false;
            }

            if (liveInstancesSize > autoscaleResource.getMinSize()) {
                isMatch = false;
                out.println(String.format("Instance size does not match for group @|bold,blue %s %s|@ (%s) and live auto scaling group @|bold,blue %s|@ (%s) ",
                        autoscaleResource.getRegion().getName(),
                        autoscaleResource.getAutoScalingGroupName(),
                        autoscaleResource.getMinSize(),
                        liveAutoscaleResource.getAutoScalingGroupName(),
                        liveInstancesSize));

                if (autoscaleResource.getMaxSize() < liveInstancesSize) {
                    out.println(String.format("Change auto scaling group @|bold,blue %s %s|@ maxSize from (%s)->(%s) ",
                            autoscaleResource.getRegion().getName(),
                            autoscaleResource.getAutoScalingGroupName(),
                            autoscaleResource.getMaxSize(),
                            liveInstancesSize));

                    autoscaleResource.setMaxSize(liveInstancesSize);
                    AutoScalingGroupResource.updateMaxSize(asClient, autoscaleResource.getAutoScalingGroupName(), autoscaleResource.getMaxSize());
                }

                out.println(String.format("Change auto scaling group @|bold,blue %s %s|@ minSize from (%s)->(%s) ",
                        autoscaleResource.getRegion().getName(),
                        autoscaleResource.getAutoScalingGroupName(),
                        autoscaleResource.getMinSize(),
                        liveInstancesSize));

                autoscaleResource.setMinSize(liveInstancesSize);
                AutoScalingGroupResource.updateMinSize(asClient, autoscaleResource.getAutoScalingGroupName(), autoscaleResource.getMinSize());
                out.println();
            }
        }

        if (!isMatch) {
            // Wait for all verification instances to be ready.
            out.println();
            for (Deployment deployment : deployments) {
                waitForAutoscaleInstances(cloud, deployment, out);
            }

            Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap = new HashMap<>();

            // Wait for all instances in verification load balancer to be in service
            for (Deployment deployment : deployments) {
                if (!deployment.getStatus().equals(Deployment.Status.PUSHED)) {
                    for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                        addNodes(cloud, deployment.getAutoscaleResource(), null, verificationLb, out, instanceMap);
                    }
                }
            }

            waitForAllLoadBalancerInstances(cloud, instanceMap);
        }

        return isMatch;
    }

    private void push(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        boolean isMatch = matchLiveInstanceSize(cloud, deployments, out);
        if (!isMatch) {
            return;
        }

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());

        Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap = new HashMap<>();

        for (Deployment deployment : deployments) {
            // Change the pending autoscale group's load balanacer configuration to point
            // the production load balancers.
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
            autoscaleResource.getLoadBalancers().clear();

            for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
            }

            asClient.setRegion(autoscaleResource.getRegion());

            detachLoadBalancer(asClient, autoscaleResource, deployment, "verify");
            attachLoadBalancer(asClient, autoscaleResource, deployment, "live");

            // Change the production autoscale group's load balancer configuration to
            // be blank so new instances do not go into the production load balancer.
            AutoScalingGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();

            if (liveAutoscaleResource != null) {
                liveAutoscaleResource.getLoadBalancers().clear();
                detachLoadBalancer(asClient, liveAutoscaleResource, deployment, "live");
            }

            // Move instances from verification to producation.
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);

                out.println(String.format("Pushing instances for group @|bold,blue %s %s|@ into production load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getRegion().getName(),
                        autoscaleResource.getAutoScalingGroupName(),
                        liveLb.getLoadBalancerName()));

                addNodes(cloud, autoscaleResource, liveAutoscaleResource, liveLb, out, instanceMap);
            }
        }

        waitForAllLoadBalancerInstances(cloud, instanceMap);

        for (Deployment deployment : deployments) {
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);

                removeNodes(cloud, deployment.getLiveAutoscaleResource(), liveLb, out);
                removeNodes(cloud, deployment.getAutoscaleResource(), verificationLb, out);
            }
        }

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
            AutoScalingGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();

            asClient.setRegion(autoscaleResource.getRegion());
            deployment.setStatus(Deployment.Status.PUSHED);
            AutoScalingGroupResource.updateVerifyingTag(asClient, autoscaleResource.getAutoScalingGroupName(), "beam.verifying", "PUSHED");
            AutoScalingGroupResource.updateVerifyingTag(asClient, autoscaleResource.getAutoScalingGroupName(), "beam.prodASName", liveAutoscaleResource.getAutoScalingGroupName());
        }

        afterPush(cloud, deployments, out);
    }

    private void revert(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());

        Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap = new HashMap<>();

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

            asClient.setRegion(autoscaleResource.getRegion());

            // Put production instances into production load balancer.

            AutoScalingGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
            if (liveAutoscaleResource != null) {
                liveAutoscaleResource.getLoadBalancers().clear();
                for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
                    liveAutoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
                }

                attachLoadBalancer(asClient, liveAutoscaleResource, deployment, "live");
            }

            // Put verification instances into verification load balancer.
            autoscaleResource.getLoadBalancers().clear();
            for (LoadBalancerResource loadBalancerResource : deployment.getVerificationLoadBalancers()) {
                autoscaleResource.getLoadBalancers().add(newReference(loadBalancerResource));
            }

            detachLoadBalancer(asClient, autoscaleResource, deployment, "live");
            attachLoadBalancer(asClient, autoscaleResource, deployment, "verify");

            // Move instances from production to verification.
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);

                out.println(String.format("Reverting instances for group @|bold,blue %s %s|@ back to verification load balancer @|bold,blue %s|@ ",
                        autoscaleResource.getRegion().getName(),
                        autoscaleResource.getAutoScalingGroupName(),
                        verificationLb.getLoadBalancerName()));

                addNodes(cloud, deployment.getLiveAutoscaleResource(), deployment.getAutoscaleResource(), liveLb, out, instanceMap);
                addNodes(cloud, deployment.getAutoscaleResource(), null, verificationLb, out, instanceMap);
            }
        }

        waitForAllLoadBalancerInstances(cloud, instanceMap);

        for (Deployment deployment : deployments) {
            for (LoadBalancerResource verificationLb : deployment.getVerificationLoadBalancers()) {
                LoadBalancerResource liveLb = deployment.matchingLiveLoadBalancer(verificationLb);
                removeNodes(cloud, deployment.getAutoscaleResource(), liveLb, out);
            }
        }

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

            asClient.setRegion(autoscaleResource.getRegion());
            deployment.setStatus(Deployment.Status.INSTANCES_CREATED);
            AutoScalingGroupResource.updateVerifyingTag(asClient, autoscaleResource.getAutoScalingGroupName(), "beam.verifying", "INSTANCES_CREATED");
        }

        afterRevert(cloud, deployments, out);
    }

    private void commit(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {

        out.print("Executing: @|red + Deleting verification load balancers |@");
        for (Deployment deployment : deployments) {
            deleteVerificationLoadBalancer(cloud, deployment, out);
        }

        out.println("OK");
        out.print("Executing: @|green + Committing autoscale group |@");
        out.flush();

        for (Deployment deployment : deployments) {
            AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

            AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
            asClient.setRegion(autoscaleResource.getRegion());

            autoscaleResource.deleteVerifyingTag(asClient, autoscaleResource.getAutoScalingGroupName());
        }

        for (AutoScalingGroupResource nonElbGroup : getNonElbGroups()) {
            AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
            asClient.setRegion(nonElbGroup.getRegion());
            AutoScalingGroupResource.deleteVerifyingTag(asClient, nonElbGroup.getAutoScalingGroupName());
        }

        afterCommit(cloud, deployments, out);
    }

    private void setExtraAttributes(Deployment deployment, Map<String, String> metadata) {
        AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

        List<String> loadbalancerNames = new ArrayList<>();
        for (LoadBalancerResource loadBalancerResource : deployment.getLiveLoadBalancers()) {
            loadbalancerNames.add(loadBalancerResource.getLoadBalancerName());
        }

        autoscaleResource.getDeployment().getExtraAttributes().put("loadbalancers", loadbalancerNames);
        autoscaleResource.getDeployment().getExtraAttributes().put("metadata", metadata);
    }

    private void afterPush(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        try {
            for (Deployment deployment : deployments) {
                AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
                Map<String, String> metadata = autoscaleResource.getMetaData(cloud);
                setExtraAttributes(deployment, metadata);
                autoscaleResource.getDeployment().afterPush();
            }

        } catch (Exception e) {
            out.println("Fail to execute afterPush: " + e.getMessage());
            out.flush();
        }
    }

    private void afterRevert(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        try {
            for (Deployment deployment : deployments) {
                AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
                AutoScalingGroupResource liveAutoscaleResource = deployment.getLiveAutoscaleResource();
                Map<String, String> metadata = liveAutoscaleResource.getMetaData(cloud);
                setExtraAttributes(deployment, metadata);
                autoscaleResource.getDeployment().afterRevert();
            }

        } catch (Exception e) {
            out.println("Fail to execute afterRevert: " + e.getMessage());
            out.flush();
        }
    }

    private void afterCommit(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        try {
            for (Deployment deployment : deployments) {
                AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();
                Map<String, String> metadata = autoscaleResource.getMetaData(cloud);
                setExtraAttributes(deployment, metadata);
                autoscaleResource.getDeployment().afterCommit();
            }

        } catch (Exception e) {
            out.println("Fail to execute afterCommit: " + e.getMessage());
            out.flush();
        }
    }

    private Set<String> groupsInLoadBalancer(AWSCloud cloud,
                                             Deployment deployment,
                                             LoadBalancerResource loadBalancerResource) {

        Set<String> groups = new HashSet<>();
        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(loadBalancerResource.getRegion());

        for (AutoScalingGroup asg : asClient.describeAutoScalingGroups().getAutoScalingGroups()) {
            if (asg.getLoadBalancerNames().contains(loadBalancerResource.getLoadBalancerName())) {
                groups.add(asg.getAutoScalingGroupName());
            }
        }

        return groups;
    }

    private void addNodes(AWSCloud cloud, AutoScalingGroupResource autoscaleResource, AutoScalingGroupResource current, LoadBalancerResource to, PrintWriter out, Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap) {

        // Register all auto scaled instances with load balancer.
        DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();

        dasgRequest.setAutoScalingGroupNames(Arrays.asList(autoscaleResource.getAutoScalingGroupName()));

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(autoscaleResource.getRegion());

        AmazonElasticLoadBalancingClient lbClient = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        lbClient.setRegion(to.getRegion());

        for (AutoScalingGroup asg : asClient.
                describeAutoScalingGroups(dasgRequest).
                getAutoScalingGroups()) {

            Set<com.amazonaws.services.elasticloadbalancing.model.Instance> newInstances = new HashSet<>();

            if (current != null && asg.getAutoScalingGroupName().equals(current.getAutoScalingGroupName())) {
                for (Instance i : asg.getInstances()) {
                    newInstances.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(i.getInstanceId()));
                }
            }

            if (asg.getAutoScalingGroupName().equals(autoscaleResource.getAutoScalingGroupName())) {
                RegisterInstancesWithLoadBalancerRequest riwlbRequest = new RegisterInstancesWithLoadBalancerRequest();

                riwlbRequest.setLoadBalancerName(to.getLoadBalancerName());

                for (Instance i : asg.getInstances()) {
                    newInstances.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(i.getInstanceId()));
                }

                riwlbRequest.setInstances(newInstances);
                lbClient.registerInstancesWithLoadBalancer(riwlbRequest);

                instanceMap.put(to.getRegion().getName() + "," + to.getLoadBalancerName(), newInstances);
            }
        }
    }

    private void removeNodes(AWSCloud cloud, AutoScalingGroupResource autoscaleResource, LoadBalancerResource from, PrintWriter out) {
        DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();

        dasgRequest.setAutoScalingGroupNames(Arrays.asList(autoscaleResource.getAutoScalingGroupName()));

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(autoscaleResource.getRegion());

        AmazonElasticLoadBalancingClient lbClient = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        lbClient.setRegion(from.getRegion());

        for (AutoScalingGroup asg : asClient.
                describeAutoScalingGroups(dasgRequest).
                getAutoScalingGroups()) {

            Set<com.amazonaws.services.elasticloadbalancing.model.Instance> newInstances = new HashSet<>();
            if (asg.getAutoScalingGroupName().equals(autoscaleResource.getAutoScalingGroupName())) {
                DeregisterInstancesFromLoadBalancerRequest driflbRequest = new DeregisterInstancesFromLoadBalancerRequest();

                driflbRequest.setLoadBalancerName(from.getLoadBalancerName());

                for (Instance i : asg.getInstances()) {
                    newInstances.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(i.getInstanceId()));
                }

                driflbRequest.setInstances(newInstances);
                lbClient.deregisterInstancesFromLoadBalancer(driflbRequest);
            }
        }
    }

    private void waitForAllLoadBalancerInstances(AWSCloud cloud, Map<String, Set<com.amazonaws.services.elasticloadbalancing.model.Instance>> instanceMap) {
        for (String lbFullName : instanceMap.keySet()) {
            String[] lbFullNameList = lbFullName.split(",");
            String region = lbFullNameList[0];
            String lbName = lbFullNameList[1];

            Set<String> lbNames = new HashSet<>();
            lbNames.add(lbName);
            Set<com.amazonaws.services.elasticloadbalancing.model.Instance> newInstances = instanceMap.get(lbFullName);

            AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(cloud.getProvider());
            lbClient.setRegion(RegionUtils.getRegion(region));

            waitForLoadBalancerInstances("Verification", lbClient, lbNames, newInstances);
        }
    }

    private void waitForLoadBalancerInstances(
            String type,
            AmazonElasticLoadBalancingClient client,
            Set<String> lbNames,
            Set<com.amazonaws.services.elasticloadbalancing.model.Instance> instances) {

        long start = System.currentTimeMillis();

        WAIT: while (true) {
            for (String lbName : lbNames) {
                DescribeInstanceHealthRequest dihRequest = new DescribeInstanceHealthRequest();

                dihRequest.setLoadBalancerName(lbName);
                dihRequest.setInstances(instances);

                for (InstanceState is : client.
                        describeInstanceHealth(dihRequest).
                        getInstanceStates()) {

                    if (!"InService".equals(is.getState())) {
                        long now = System.currentTimeMillis();

                        if (now - start > 60000) {
                            start = now;

                            System.out.print(String.format("%s: Keep waiting? (Y/n) ", type));
                            System.out.flush();

                            BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                            try {
                                if ("n".equalsIgnoreCase(confirmReader.readLine())) {
                                    return;
                                }

                            } catch (IOException error) {
                                throw Throwables.propagate(error);
                            }
                        }

                        System.out.println(String.format(
                                "%s: Waiting for load balancer instances to be InService...",
                                type));

                        try {
                            Thread.sleep(10000);

                        } catch (InterruptedException error) {
                            return;
                        }

                        continue WAIT;
                    }
                }
            }

            break;
        }
    }

    private void waitForAutoscaleInstances(AWSCloud cloud, Deployment deployment, PrintWriter out) {
        // Wait for all instances to be running.
        Set<com.amazonaws.services.elasticloadbalancing.model.Instance> newInstances = new HashSet<>();

        AutoScalingGroupResource autoscaleResource = deployment.getAutoscaleResource();

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        asClient.setRegion(autoscaleResource.getRegion());

        DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();
        dasgRequest.setAutoScalingGroupNames(Arrays.asList(autoscaleResource.getAutoScalingGroupName()));

        int minimumSize = autoscaleResource.getMinSize();

        while (true) {
            int pendingSize = 0;
            int currentSize = 0;

            for (AutoScalingGroup asg : asClient.
                    describeAutoScalingGroups(dasgRequest).
                    getAutoScalingGroups()) {

                for (Instance i : asg.getInstances()) {
                    String state = i.getLifecycleState();
                    String instanceId = i.getInstanceId();

                    if (LifecycleState.Pending.toString().equals(state)) {
                        ++ pendingSize;
                    }

                    if (!TERMINATING_LIFECYCLE_STATES.contains(state)) {
                        ++ currentSize;

                        newInstances.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId));
                    }
                }
            }

            if (currentSize >= minimumSize) {
                if (pendingSize > 0) {
                    out.println(String.format(
                            "Verification: Waiting for %d instances to be in service in %s",
                            pendingSize,
                            autoscaleResource.getAutoScalingGroupName()));

                } else {
                    break;
                }

            } else {
                out.println(String.format(
                        "Verification: Waiting for %d instances (%d current) to be running in %s",
                        minimumSize,
                        currentSize,
                        autoscaleResource.getAutoScalingGroupName()));
            }

            try {
                Thread.sleep(10000);

            } catch (InterruptedException error) {
                break;
            }
        }
    }

    private void debug(AWSCloud cloud, List<Deployment> deployments, PrintWriter out) {
        for (Deployment deployment : deployments) {
            System.out.println("     ASG: " + deployment.getAutoscaleResource().getAutoScalingGroupName());

            if (deployment.getLiveAutoscaleResource() != null) {
                System.out.println("Live ASG: " + deployment.getLiveAutoscaleResource().getAutoScalingGroupName());
            }
        }
    }

    private void setDeploymentStatus(AutoScalingGroupResource autoscaleResource, Deployment deployment) {
        String status = "UNDEPLOYED";
        for (AutoScalingGroupTagResource tag : autoscaleResource.getTags()) {
            if ("beam.verifying".equals(tag.getKey())) {
                status = tag.getValue();
            }
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
        for (AutoScalingGroupResource asgResource : getAutoscaleGroups()) {
            for (AutoScalingGroupTagResource tag : asgResource.getTags()) {
                if ("beam.layer".equals(tag.getKey())) {
                    builder.append("[" + asgResource.getRegion().getName() + " " + tag.getValue() + "]" + " ");
                }
            }
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

