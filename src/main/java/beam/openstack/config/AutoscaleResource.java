package beam.openstack.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.config.ConfigValue;
import beam.config.DeploymentConfig;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import com.psddev.dari.util.StringUtils;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.*;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;

import java.util.*;

@ConfigValue("openstack")
public class AutoscaleResource extends OpenStackResource<Group> {

    private String groupId;
    private String name;
    private int coolDown;
    private int maxEntities;
    private int minEntities;
    private GroupConfiguration config;
    private LaunchConfigurationResource launchConfig;
    private List<ScalingPolicyResource> policies;
    private Map<String, String> metadata;
    private String groupHash;

    private transient List<Server> servers = null;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    @ResourceDiffProperty(updatable = true)
    public int getMaxEntities() {
        return maxEntities;
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    @ResourceDiffProperty(updatable = true)
    public int getMinEntities() {
        return minEntities;
    }

    public void setMinEntities(int minEntities) {
        this.minEntities = minEntities;
    }

    public GroupConfiguration getConfig() {
        return config;
    }

    public void setConfig(GroupConfiguration config) {
        this.config = config;
    }

    public LaunchConfigurationResource getLaunchConfig() {
        return launchConfig;
    }

    public void setLaunchConfig(LaunchConfigurationResource launchConfig) {
        this.launchConfig = launchConfig;
    }

    public List<ScalingPolicyResource> getPolicies() {
        if (policies == null) {
            policies = new ArrayList<>();
        }

        return policies;
    }

    public void setPolicies(List<ScalingPolicyResource> policies) {
        this.policies = policies;
    }

    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        return metadata;
    }

    public String getMetadataItem(String key) {
        return getMetadata().get(key);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getGroupHash() {
        return groupHash;
    }

    public void setGroupHash(String groupHash) {
        this.groupHash = groupHash;
    }

    public String getGroupPrefix() {
        return String.format("%s %s %s v%s",
                getMetadataItem("project"),
                getMetadataItem("layer"),
                getMetadataItem("environment"),
                getMetadataItem("serial"));
    }

    public List<Server> getServers(OpenStackCloud cloud) {
        if (servers != null) {
            return servers;
        }

        servers = new ArrayList<>();

        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(getRegion());

        NovaApi novaApi = cloud.createApi();
        ServerApi serverApi = novaApi.getServerApi(getRegion());

        GroupState state = groupApi.getState(getGroupId());

        Set<String> deviceIds = new HashSet<>();
        for (GroupInstance instance : state.getGroupInstances()) {
            deviceIds.add(instance.getId());
        }

        for (Server server : serverApi.listInDetail().concat()) {
            if (deviceIds.contains(server.getId())) {
                servers.add(server);
            }
        }

        return servers;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Group group) {
        config = group.getGroupConfiguration();

        setName(config.getName());
        setCoolDown(config.getCooldown());
        setMinEntities(config.getMinEntities());
        setMaxEntities(config.getMaxEntities());
        setMetadata(new HashMap(config.getMetadata()));
        setGroupId(group.getId());

        for (ScalingPolicy policy : group.getScalingPolicies()) {
            ScalingPolicyResource policyResource = new ScalingPolicyResource();
            policyResource.setRegion(getRegion());
            policyResource.init(cloud, filter, policy);

            getPolicies().add(policyResource);
        }

        LaunchConfiguration launchConfiguration = group.getLaunchConfiguration();
        if (launchConfiguration != null) {
            LaunchConfigurationResource lcResource = new LaunchConfigurationResource();
            lcResource.setRegion(getRegion());
            lcResource.init(cloud, filter, launchConfiguration);

            setLaunchConfig(lcResource);
        }

        StringBuilder groupHashBuilder = new StringBuilder();
        appendHash(groupHashBuilder, "image", getLaunchConfig().getImage());
        appendHash(groupHashBuilder, "instanceType", getLaunchConfig().getFlavor());

        try {
            if (!getMetadata().containsKey("type")) {
                appendHash(groupHashBuilder, "buildNumber", getMetadata().get("buildNumber"));
                appendHash(groupHashBuilder, "jenkinsBucket", getMetadata().get("jenkinsBucket"));
                appendHash(groupHashBuilder, "jenkinsBuildPath", getMetadata().get("jenkinsBuildPath"));
                appendHash(groupHashBuilder, "jenkinsWarFile", getMetadata().get("jenkinsWarFile"));

            } else {
                String pluginName = getMetadata().get("type");
                Class<?> plugin = Class.forName(pluginName);
                DeploymentConfig deployment = (DeploymentConfig)plugin.getConstructor().newInstance();

                for (String key : deployment.getGroupHashKeys()) {
                    String value = getMetadata().get(key);
                    appendHash(groupHashBuilder, key, value);
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
        }

        String groupHash = StringUtils.hex(StringUtils.md5(groupHashBuilder.toString()));
        setGroupHash(groupHash);
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.createOne(getLaunchConfig());
        create.create(getPolicies());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Group> current) throws Exception {
        AutoscaleResource autoscaleResource = (AutoscaleResource) current;

        update.updateOne(autoscaleResource.getLaunchConfig(), getLaunchConfig());
        update.update(autoscaleResource.getPolicies(), getPolicies());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.deleteOne(getLaunchConfig());
        delete.delete(getPolicies());
    }

    @Override
    public void create(OpenStackCloud cloud) {
        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(getRegion());

        Map<String, String> verifyingMetadata = getMetadata();
        GroupConfiguration groupConfig = GroupConfiguration.builder()
                .maxEntities(getMaxEntities())
                .minEntities(getMinEntities())
                .cooldown(getCoolDown())
                .metadata(verifyingMetadata)
                .name(getName())
                .build();

        LaunchConfiguration launchConfiguration = getLaunchConfig().buildLaunchConfiguration();

        // Make sure server image has "auto_disk_config" set to true.
        NovaApi novaApi = cloud.createApi();
        ImageApi imageApi = novaApi.getImageApi(getRegion());

        Image image = imageApi.get(launchConfiguration.getServerImageRef());
        if (image != null) {
            Map<String, String> metadata = new HashMap(image.getMetadata());
            metadata.put("auto_disk_config", "True");

            imageApi.updateMetadata(image.getId(), metadata);
        }

        List<CreateScalingPolicy> scalingPolicies = new ArrayList<>();
        for (ScalingPolicyResource policyResource : getPolicies()) {
            scalingPolicies.add(policyResource.buildScalingPolicy());
        }

        Group group = groupApi.create(groupConfig, launchConfiguration, scalingPolicies);
        setGroupId(group.getId());

        for (ScalingPolicy policy : group.getScalingPolicies()) {
            ScalingPolicyResource policyResource = new ScalingPolicyResource();
            policyResource.setRegion(getRegion());
            policyResource.init(cloud, null, policy);

            getPolicies().add(policyResource);
        }

        launchConfiguration = group.getLaunchConfiguration();
        if (launchConfiguration != null) {
            LaunchConfigurationResource lcResource = new LaunchConfigurationResource();
            lcResource.setRegion(getRegion());
            lcResource.init(cloud, null, launchConfiguration);

            setLaunchConfig(lcResource);
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Group> current, Set<String> changedProperties) {
        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(getRegion());

        GroupConfiguration groupConfig = GroupConfiguration.builder()
                .maxEntities(getMaxEntities())
                .minEntities(getMinEntities())
                .cooldown(getCoolDown())
                .metadata(getMetadata())
                .name(getName())
                .build();

        groupApi.updateGroupConfiguration(getGroupId(), groupConfig);
    }

    @Override
    public void delete(OpenStackCloud cloud) {
        AutoscaleApi api = cloud.createAutoscaleApi();
        GroupApi groupApi = api.getGroupApi(getRegion());

        GroupConfiguration groupConfig = GroupConfiguration.builder()
                .maxEntities(0)
                .minEntities(0)
                .cooldown(getCoolDown())
                .metadata(getMetadata())
                .name(getName())
                .build();

        groupApi.updateGroupConfiguration(getGroupId(), groupConfig);
        groupApi.delete(getGroupId());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String toDisplayString() {
        return "auto scaling group " + getName();
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
    }
}
