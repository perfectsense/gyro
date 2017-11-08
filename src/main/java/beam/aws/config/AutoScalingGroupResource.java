package beam.aws.config;

import java.util.*;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.config.DeploymentConfig;
import beam.diff.NullSet;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public class AutoScalingGroupResource extends AWSResource<AutoScalingGroup> {

    private static final String NOTIFICATIONS_QUEUE_NAME = "auto-scaling-group-notifications";
    private static final String NOTIFICATIONS_QUEUE_ROLE_NAME = "auto-scaling-group-notifications";
    private static final String NOTIFICATIONS_QUEUE_ROLE_POLICY_NAME = "sqs";

    private static final Set<String> TERMINATING_LIFECYCLE_STATES = ImmutableSet.of(
            LifecycleState.Terminated.toString(),
            LifecycleState.Terminating.toString(),
            LifecycleState.TerminatingProceed.toString(),
            LifecycleState.TerminatingWait.toString());

    private static final String VERIFICATION_HOOK_NAME = "wait-for-verification";

    private String autoScalingGroupName;
    private Integer defaultCooldown;
    private Integer healthCheckGracePeriod;
    private String healthCheckType;
    private BeamReference launchConfiguration;
    private Set<BeamReference> loadBalancers;
    private Integer maxSize;
    private Integer minSize;
    private String placementGroup;
    private Set<AutoScalingGroupPolicyResource> policies;
    private List<AutoScalingGroupScheduleResource> schedules;
    private Set<BeamReference> subnets;
    private Set<AutoScalingGroupTagResource> tags;
    private BeamReference hostedZone;
    private String groupHash;
    DeploymentConfig deployment;

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public void setAutoScalingGroupName(String autoScalingGroupName) {
        this.autoScalingGroupName = autoScalingGroupName;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getDefaultCooldown() {
        return defaultCooldown;
    }

    public void setDefaultCooldown(Integer defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getHealthCheckGracePeriod() {
        return healthCheckGracePeriod;
    }

    public void setHealthCheckGracePeriod(Integer healthCheckGracePeriod) {
        this.healthCheckGracePeriod = healthCheckGracePeriod;
    }

    @ResourceDiffProperty(updatable = true)
    public String getHealthCheckType() {
        return healthCheckType;
    }

    public void setHealthCheckType(String healthCheckType) {
        this.healthCheckType = healthCheckType;
    }

    @ResourceDiffProperty(updatable = true)
    public BeamReference getLaunchConfiguration() {
        return launchConfiguration;
    }

    public void setLaunchConfiguration(BeamReference launchConfiguration) {
        this.launchConfiguration = launchConfiguration;
    }

    @ResourceDiffProperty(updatable = true)
    public Set<BeamReference> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new NullSet<>();
        }
        return loadBalancers;
    }

    public void setLoadBalancers(Set<BeamReference> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(String placementGroup) {
        this.placementGroup = placementGroup;
    }

    public Set<AutoScalingGroupPolicyResource> getPolicies() {
        if (policies == null) {
            policies = new HashSet<>();
        }
        return policies;
    }

    public void setPolicies(Set<AutoScalingGroupPolicyResource> policies) {
        this.policies = policies;
    }

    public List<AutoScalingGroupScheduleResource> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<AutoScalingGroupScheduleResource> schedules) {
        this.schedules = schedules;
    }

    @ResourceDiffProperty(updatable = true)
    public Set<BeamReference> getSubnets() {
        if (subnets == null) {
            subnets = new NullSet<>();
        }
        return subnets;
    }

    public void setSubnets(Set<BeamReference> subnets) {
        this.subnets = subnets;
    }

    /**
     * @return Never {@code null}.
     */
    public Set<AutoScalingGroupTagResource> getTags() {
        if (tags == null) {
            tags = new HashSet<>();
        }
        return tags;
    }

    public void setTags(Set<AutoScalingGroupTagResource> tags) {
        this.tags = tags;
    }

    public BeamReference getHostedZone() {
        return hostedZone;
    }

    public void setHostedZone(BeamReference hostedZone) {
        this.hostedZone = hostedZone;
    }

    public String getGroupHash() {
        return groupHash;
    }

    public void setGroupHash(String groupHash) {
        this.groupHash = groupHash;
    }

    public DeploymentConfig getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentConfig deployment) {
        this.deployment = deployment;
    }

    @Override
    public String awsId() {
        return getAutoScalingGroupName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getAutoScalingGroupName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, AutoScalingGroup group) {
        String lcName = group.getLaunchConfigurationName();

        setAutoScalingGroupName(group.getAutoScalingGroupName());
        setDefaultCooldown(group.getDefaultCooldown());
        setHealthCheckGracePeriod(group.getHealthCheckGracePeriod());
        setHealthCheckType(group.getHealthCheckType());
        setLaunchConfiguration(newReference(LaunchConfigurationResource.class, lcName));
        setLoadBalancers(newReferenceSet(LoadBalancerResource.class, group.getLoadBalancerNames()));
        setMaxSize(group.getMaxSize());
        setMinSize(group.getMinSize());
        setPlacementGroup(group.getPlacementGroup());

        // Subnets.
        String subnetIds = group.getVPCZoneIdentifier();

        if (subnetIds != null) {
            for (String subnetId : subnetIds.trim().split("\\s*,\\s*")) {
                getSubnets().add(newReference(SubnetResource.class, subnetId));
            }
        }

        // Policies.
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        DescribePoliciesRequest dpRequest = new DescribePoliciesRequest();

        dpRequest.setAutoScalingGroupName(getAutoScalingGroupName());

        for (ScalingPolicy policy : client.
                describePolicies(dpRequest).
                getScalingPolicies()) {

            if (isInclude(filter, policy)) {
                AutoScalingGroupPolicyResource policyResource = new AutoScalingGroupPolicyResource();
                policyResource.setRegion(getRegion());

                policyResource.init(cloud, filter, policy);
                getPolicies().add(policyResource);
            }
        }

        // Schedules.
        DescribeScheduledActionsRequest dsaRequest = new DescribeScheduledActionsRequest();
        dsaRequest.setAutoScalingGroupName(getAutoScalingGroupName());

        List<AutoScalingGroupScheduleResource> newSchedules = new ArrayList<>();
        for (ScheduledUpdateGroupAction schedule : client.
                describeScheduledActions(dsaRequest).
                getScheduledUpdateGroupActions()) {

            if (isInclude(filter, schedule)) {
                AutoScalingGroupScheduleResource scheduleResource = new AutoScalingGroupScheduleResource();

                scheduleResource.init(cloud, filter, schedule);
                newSchedules.add(scheduleResource);
            }
        }

        if (!newSchedules.isEmpty()) {
            schedules = newSchedules;
        }

        // Tags.
        for (TagDescription td : group.getTags()) {
            AutoScalingGroupTagResource tag = new AutoScalingGroupTagResource();

            getTags().add(tag);
            tag.setKey(td.getKey());
            tag.setPropagateAtLaunch(td.getPropagateAtLaunch());
            tag.setValue(td.getValue());
            tag.setRegion(getRegion());
        }

        Map<String, String> metaData = getMetaData(cloud);
        metaData.remove("instanceType");

        StringBuilder groupHashBuilder = new StringBuilder();
        for (String key : metaData.keySet()) {
            String value = metaData.get(key);
            appendHash(groupHashBuilder, key, value);
        }

        String groupHash = StringUtils.hex(StringUtils.md5(groupHashBuilder.toString()));
        setGroupHash(groupHash);
    }

    public Map<String, String> getMetaData(AWSCloud cloud) {
        DescribeLaunchConfigurationsRequest dlcRequest = new DescribeLaunchConfigurationsRequest();
        dlcRequest.setLaunchConfigurationNames(Arrays.asList(getLaunchConfiguration().awsId()));

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        LaunchConfiguration config = asClient.describeLaunchConfigurations(dlcRequest).getLaunchConfigurations().get(0);

        Map<String, String> metaData = new LinkedHashMap<>();

        String userdataJson = new String(BaseEncoding.base64().decode(config.getUserData()));
        Map<String, String> rawData = (Map<String, String>) ObjectUtils.fromJson(userdataJson);

        String imageName = "";
        AmazonEC2Client ec2Client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DescribeImagesRequest diRequest = new DescribeImagesRequest();
        diRequest.setImageIds(Arrays.asList(config.getImageId()));

        for (Image image : ec2Client.describeImages(diRequest).getImages()) {
            imageName = image.getName();
        }

        metaData.put("image", imageName);
        metaData.put("instanceType", config.getInstanceType());

        if (!rawData.containsKey("type")) {
            String warUrl = rawData.get("war_file");
            if (!ObjectUtils.isBlank(warUrl)) {
                int beginIndex = warUrl.indexOf("s3://") + "s3://".length();
                int endIndex = warUrl.indexOf("/production-builds/");
                if (endIndex == -1) {
                    endIndex = warUrl.indexOf("/builds/");
                }

                String bucketName = warUrl.substring(beginIndex, endIndex);
                String key = warUrl.substring(endIndex + 1);

                AmazonS3Client s3Client = new AmazonS3Client(cloud.getProvider());
                S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));

                S3ObjectResource objectResource = new S3ObjectResource();
                objectResource.init(cloud, null, object);

                String objectContentUrl = objectResource.getObjectContentUrl();
                String[] contents = objectContentUrl.substring(beginIndex).split("/");

                String jenkinsBucket = contents[0];
                String jenkinsWarFile = contents[contents.length - 1];
                String buildNumber = contents[contents.length - 2];
                String jenkinsBuildPath = "";

                for (int i = 1; i < contents.length - 2; i++) {
                    jenkinsBuildPath = jenkinsBuildPath + contents[i] + "/";
                }

                jenkinsBuildPath = jenkinsBuildPath.substring(0, jenkinsBuildPath.length() - 1);

                metaData.put("buildNumber", buildNumber);
                metaData.put("jenkinsBucket", jenkinsBucket);
                metaData.put("jenkinsBuildPath", jenkinsBuildPath);
                metaData.put("jenkinsWarFile", jenkinsWarFile);
            }

        } else {
            try {
                String pluginName = rawData.get("type");
                Class<?> plugin = Class.forName(pluginName);
                DeploymentConfig deployment = (DeploymentConfig)plugin.getConstructor().newInstance();

                for (String key : deployment.getGroupHashKeys()) {
                    String value = rawData.get(key);
                    metaData.put(key, value);
                }

            } catch (Exception error) {
                error.printStackTrace();
            }
        }

        return metaData;
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getPolicies());
        create.create(getSchedules());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, AutoScalingGroup> current) throws Exception {
        AutoScalingGroupResource currentAsg = (AutoScalingGroupResource) current;

        update.update(currentAsg.getPolicies(), getPolicies());
        update.update(currentAsg.getSchedules(), getSchedules());
        update.update(currentAsg.getTags(), getTags());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getPolicies());
        delete.delete(getSchedules());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        CreateAutoScalingGroupRequest casgRequest = new CreateAutoScalingGroupRequest();
        String asgName = getAutoScalingGroupName();

        casgRequest.setAutoScalingGroupName(asgName);
        casgRequest.setDefaultCooldown(getDefaultCooldown());
        casgRequest.setHealthCheckGracePeriod(getHealthCheckGracePeriod());
        casgRequest.setHealthCheckType(getHealthCheckType());
        casgRequest.setLaunchConfigurationName(getLaunchConfiguration().awsId());
        casgRequest.setLoadBalancerNames(awsIdSet(getLoadBalancers()));
        casgRequest.setMaxSize(getMaxSize());
        casgRequest.setMinSize(0);
        casgRequest.setPlacementGroup(getPlacementGroup());
        casgRequest.setVPCZoneIdentifier(Joiner.on(',').join(awsIdSet(getSubnets())));

        // Tags.
        for (AutoScalingGroupTagResource tag : getTags()) {
            casgRequest.getTags().add(tag.toTag());
        }

        // Mark the auto scaling group as verifying so that the process can
        // be resumed later.
        Tag verifyingTag = new Tag().
                withKey("beam.verifying").
                withPropagateAtLaunch(Boolean.FALSE).
                withResourceId(asgName).
                withResourceType("auto-scaling-group").
                withValue("UNDEPLOYED");

        Tag prodASNameTag = new Tag().
                withKey("beam.prodASName").
                withPropagateAtLaunch(Boolean.FALSE).
                withResourceId(asgName).
                withResourceType("auto-scaling-group").
                withValue("");

        casgRequest.getTags().add(verifyingTag);
        casgRequest.getTags().add(prodASNameTag);
        client.createAutoScalingGroup(casgRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, AutoScalingGroup> current, Set<String> changedProperties) {

        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        UpdateAutoScalingGroupRequest uasgRequest = new UpdateAutoScalingGroupRequest();

        uasgRequest.setAutoScalingGroupName(getAutoScalingGroupName());
        uasgRequest.setDefaultCooldown(getDefaultCooldown());
        uasgRequest.setHealthCheckGracePeriod(getHealthCheckGracePeriod());
        uasgRequest.setHealthCheckType(getHealthCheckType());
        uasgRequest.setLaunchConfigurationName(getLaunchConfiguration().awsId());
        uasgRequest.setMaxSize(getMaxSize());

        boolean isVerify = false;
        for (AutoScalingGroupTagResource tag : ((AutoScalingGroupResource) current).getTags()) {
            if (ObjectUtils.to(boolean.class, "beam.verifying".equals(tag.getKey()))) {
                isVerify = true;
            }
        }

        if (isVerify) {
            System.out.print(" [SKIPPING update minSize or loadBalancers] ");
            uasgRequest.setMinSize(((AutoScalingGroupResource) current).getMinSize());
        } else {
            uasgRequest.setMinSize(getMinSize());
            updateLoadBalancers(cloud, ((AutoScalingGroupResource) current).getLoadBalancers(), getLoadBalancers());
        }

        uasgRequest.setPlacementGroup(getPlacementGroup());
        uasgRequest.setVPCZoneIdentifier(Joiner.on(',').join(awsIdSet(getSubnets())));
        client.updateAutoScalingGroup(uasgRequest);

        ((AutoScalingGroupResource) current).setMinSize(getMinSize());
    }

    private void updateLoadBalancers(AWSCloud cloud, Set<BeamReference> currentElbs, Set<BeamReference> pendingElbs) {
        Set<String> currentElbNames = new HashSet<>();
        Set<String> pendingElbNames = new HashSet<>();

        for (BeamReference elb : currentElbs) {
            currentElbNames.add(elb.awsId());
        }

        for (BeamReference elb : pendingElbs) {
            pendingElbNames.add(elb.awsId());
        }

        AmazonAutoScalingClient asClient = createClient(AmazonAutoScalingClient.class, cloud.getProvider());

        Set<String> attachElbNames = new HashSet<>(pendingElbNames);
        attachElbNames.removeAll(currentElbNames);

        if (!attachElbNames.isEmpty()) {
            AttachLoadBalancersRequest albRequest = new AttachLoadBalancersRequest();
            albRequest.setAutoScalingGroupName(getAutoScalingGroupName());
            albRequest.setLoadBalancerNames(attachElbNames);
            asClient.attachLoadBalancers(albRequest);
        }

        Set<String> detachElbNames = new HashSet<>(currentElbNames);
        detachElbNames.removeAll(pendingElbNames);

        if (!detachElbNames.isEmpty()) {
            DetachLoadBalancersRequest dlbRequest = new DetachLoadBalancersRequest();
            dlbRequest.setAutoScalingGroupName(getAutoScalingGroupName());
            dlbRequest.setLoadBalancerNames(detachElbNames);
            asClient.detachLoadBalancers(dlbRequest);
        }
    }

    public static void updateVerifyingTag(AmazonAutoScalingClient client, String asgName, String key, String value) {
        CreateOrUpdateTagsRequest coutRequest = new CreateOrUpdateTagsRequest();

        coutRequest.getTags().add(new Tag().withKey(key).
                withValue(value).
                withResourceId(asgName).
                withResourceType("auto-scaling-group").
                withPropagateAtLaunch(false));

        client.createOrUpdateTags(coutRequest);
    }

    public static void deleteVerifyingTag(AmazonAutoScalingClient client, String asgName) {
        DeleteTagsRequest dtRequest = new DeleteTagsRequest();

        dtRequest.getTags().add(new Tag().
                withKey("beam.verifying").
                withResourceId(asgName).
                withResourceType("auto-scaling-group"));

        dtRequest.getTags().add(new Tag().
                withKey("beam.prodASName").
                withResourceId(asgName).
                withResourceType("auto-scaling-group"));

        client.deleteTags(dtRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        String asgName = getAutoScalingGroupName();

        // Update the auto scaling group to have no instances.
        UpdateAutoScalingGroupRequest uasgRequest = new UpdateAutoScalingGroupRequest();

        uasgRequest.setAutoScalingGroupName(asgName);
        uasgRequest.setMaxSize(0);
        uasgRequest.setMinSize(0);
        client.updateAutoScalingGroup(uasgRequest);

        // Delete the auto scaling group.
        DeleteAutoScalingGroupRequest deasgRequest = new DeleteAutoScalingGroupRequest();

        deasgRequest.setAutoScalingGroupName(getAutoScalingGroupName());
        deasgRequest.setForceDelete(true);

        while (true) {
            try {
                client.deleteAutoScalingGroup(deasgRequest);
                break;

            } catch (ScalingActivityInProgressException saipError) {
                try {
                    Thread.sleep(2000);

                } catch (InterruptedException iError) {
                    break;
                }
            }
        }

        // Wait for autoscale group to be deleted.
        DescribeAutoScalingGroupsRequest dasgRequest = new DescribeAutoScalingGroupsRequest();
        dasgRequest.setAutoScalingGroupNames(Arrays.asList(asgName));

        DescribeAutoScalingGroupsResult dasgResult = client.describeAutoScalingGroups(dasgRequest);

        while (dasgResult.getAutoScalingGroups() == null ||
                dasgResult.getAutoScalingGroups().size() != 0) {

            try {
                dasgResult = client.describeAutoScalingGroups(dasgRequest);
                Thread.sleep(5000);

            } catch (com.amazonaws.AmazonServiceException ase) {
                break;

            } catch (InterruptedException error) {
                break;
            }
        }

    }

    public List<HostedZoneRRSetResource> findCurrentRecords(AWSCloud cloud) {
        List<HostedZoneRRSetResource> records = new ArrayList<>();

        HostedZoneResource hzResource = (HostedZoneResource) hostedZone.resolve();

        // BeamResource record sets.
        AmazonRoute53Client client = createClient(AmazonRoute53Client.class, cloud.getProvider());
        ListResourceRecordSetsRequest lrrsRequest = new ListResourceRecordSetsRequest();

        lrrsRequest.setHostedZoneId(hzResource.getId());
        ListResourceRecordSetsResult lrrsResult;

        do {
            lrrsResult = client.listResourceRecordSets(lrrsRequest);
            for (ResourceRecordSet rrs : lrrsResult.getResourceRecordSets()) {
                String type = rrs.getType();

                if (!"NS".equals(type) && !"SOA".equals(type)) {

                    HostedZoneRRSetResource rrsResource = new HostedZoneRRSetResource();

                    rrsResource.init(cloud, null, rrs);
                    records.add(rrsResource);
                }
            }

            lrrsRequest.setStartRecordIdentifier(lrrsResult.getNextRecordIdentifier());
            lrrsRequest.setStartRecordName(lrrsResult.getNextRecordName());
            lrrsRequest.setStartRecordType(lrrsResult.getNextRecordType());
        } while (lrrsResult.isTruncated());

        return records;
    }

    public static void updateMinSize(AmazonAutoScalingClient client, String asgName, int minSize) {
        UpdateAutoScalingGroupRequest uasgRequest = new UpdateAutoScalingGroupRequest();

        uasgRequest.setAutoScalingGroupName(asgName);
        uasgRequest.setMinSize(minSize);
        client.updateAutoScalingGroup(uasgRequest);
    }

    public static void updateMaxSize(AmazonAutoScalingClient client, String asgName, int maxSize) {
        UpdateAutoScalingGroupRequest uasgRequest = new UpdateAutoScalingGroupRequest();

        uasgRequest.setAutoScalingGroupName(asgName);
        uasgRequest.setMaxSize(maxSize);
        client.updateAutoScalingGroup(uasgRequest);
    }

    @Override
    public String toDisplayString() {
        return "auto scaling group " + getAutoScalingGroupName();
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
    }
}
