package beam.aws.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullSet;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.ApplySecurityGroupsToLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CrossZoneLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DetachLoadBalancerFromSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.Tag;
import com.amazonaws.services.elasticloadbalancing.model.TagDescription;
import com.amazonaws.services.elasticloadbalancing.model.ConnectionSettings;
import com.psddev.dari.util.CompactMap;

public class LoadBalancerResource extends AWSResource<LoadBalancerDescription> implements Taggable {

    private String canonicalHostedZoneNameId;
    private Boolean crossZoneLoadBalancing;
    private String dnsName;
    private LoadBalancerHealthCheckResource healthCheck;
    private Set<LoadBalancerListenerResource> listeners;
    private String loadBalancerName;
    private String scheme;
    private Set<BeamReference> subnets;
    private Set<BeamReference> securityGroups;
    private Map<String, String> tags;
    private Set<String> verificationHostnames;
    private Integer idleTimeout;

    public String getCanonicalHostedZoneNameId() {
        return canonicalHostedZoneNameId;
    }

    public void setCanonicalHostedZoneNameId(String canonicalHostedZoneNameId) {
        this.canonicalHostedZoneNameId = canonicalHostedZoneNameId;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getCrossZoneLoadBalancing() {
        return crossZoneLoadBalancing;
    }

    public void setCrossZoneLoadBalancing(Boolean crossZoneLoadBalancing) {
        this.crossZoneLoadBalancing = crossZoneLoadBalancing;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public LoadBalancerHealthCheckResource getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(LoadBalancerHealthCheckResource healthCheck) {
        this.healthCheck = healthCheck;
    }

    public Set<LoadBalancerListenerResource> getListeners() {
        if (listeners == null) {
            listeners = new HashSet<>();
        }
        return listeners;
    }

    public void setListeners(Set<LoadBalancerListenerResource> listeners) {
        this.listeners = listeners;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    @ResourceDiffProperty
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * @return Never {@code null}.
     */
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
    @ResourceDiffProperty(updatable = true)
    public Set<BeamReference> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new NullSet<>();
        }
        return securityGroups;
    }

    public void setSecurityGroups(Set<BeamReference> securityGroups) {
        this.securityGroups = securityGroups;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Set<String> getVerificationHostnames() {
        if (verificationHostnames == null) {
            verificationHostnames = new HashSet<>();
        }

        return verificationHostnames;
    }

    public void setVerificationHostnames(Set<String> verificationHostnames) {
        this.verificationHostnames = verificationHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    @Override
    public String awsId() {
        return getLoadBalancerName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getLoadBalancerName());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, LoadBalancerDescription lb) {
        String name = lb.getLoadBalancerName();

        String az = lb.getAvailabilityZones().get(0);
        String regionName = az.substring(0, az.length() - 1);
        Region region = RegionUtils.getRegion(regionName);

        setCanonicalHostedZoneNameId(lb.getCanonicalHostedZoneNameID());
        setDnsName(lb.getDNSName());
        setLoadBalancerName(name);
        setScheme(lb.getScheme());
        setSecurityGroups(newReferenceSet(SecurityGroupResource.class, lb.getSecurityGroups()));
        setSubnets(newReferenceSet(SubnetResource.class, lb.getSubnets()));
        setRegion(region);

        // Health check.
        LoadBalancerHealthCheckResource hcResource = new LoadBalancerHealthCheckResource();
        hcResource.setRegion(region);

        hcResource.init(cloud, filter, lb.getHealthCheck());
        setHealthCheck(hcResource);

        // Listeners.
        getListeners().clear();

        for (ListenerDescription ld : lb.getListenerDescriptions()) {
            if (isInclude(filter, ld)) {
                LoadBalancerListenerResource listenerResource = new LoadBalancerListenerResource();
                listenerResource.setRegion(getRegion());

                listenerResource.setLoadBalancer(newReference(LoadBalancerResource.class, getLoadBalancerName()));
                listenerResource.setPolicyNames(ld.getPolicyNames());
                listenerResource.init(cloud, filter, ld.getListener());
                getListeners().add(listenerResource);
            }
        }

        // Tags.
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        DescribeTagsRequest dtRequest = new DescribeTagsRequest();

        dtRequest.setLoadBalancerNames(Arrays.asList(getLoadBalancerName()));

        for (TagDescription td : client.
                describeTags(dtRequest).
                getTagDescriptions()) {

            for (Tag tag : td.getTags()) {
                getTags().put(tag.getKey(), tag.getValue());
            }
        }

        // Attributes.
        DescribeLoadBalancerAttributesRequest dlbaRequest = new DescribeLoadBalancerAttributesRequest();

        dlbaRequest.setLoadBalancerName(name);

        LoadBalancerAttributes attrs = client.describeLoadBalancerAttributes(dlbaRequest).getLoadBalancerAttributes();

        setCrossZoneLoadBalancing(attrs.getCrossZoneLoadBalancing().isEnabled());
        setIdleTimeout(attrs.getConnectionSettings().getIdleTimeout());
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getListeners());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AWSCloud, LoadBalancerDescription> current) throws Exception {
        LoadBalancerResource currentLb = (LoadBalancerResource) current;

        update.updateOne(currentLb.getHealthCheck(), getHealthCheck());
        update.update(currentLb.getListeners(), getListeners());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        CreateLoadBalancerRequest clbRequest = new CreateLoadBalancerRequest();
        String name = getLoadBalancerName();

        clbRequest.setLoadBalancerName(name);

        for (LoadBalancerListenerResource listenerResource : getListeners()) {
            clbRequest.getListeners().add(listenerResource.toListener());
        }

        clbRequest.setScheme(getScheme());
        clbRequest.setSecurityGroups(awsIdSet(getSecurityGroups()));
        clbRequest.setSubnets(awsIdSet(getSubnets()));

        // Tags.
        for (Map.Entry<String, String> entry : getTags().entrySet()) {
            clbRequest.getTags().add(new Tag().
                    withKey(entry.getKey()).
                    withValue(entry.getValue()));
        }

        client.createLoadBalancer(clbRequest);

        // Read from AWS to get the calculated fields.
        DescribeLoadBalancersRequest dlbRequest = new DescribeLoadBalancersRequest();

        dlbRequest.setLoadBalancerNames(Arrays.asList(name));

        for (LoadBalancerDescription lb : client.
                describeLoadBalancers(dlbRequest).
                getLoadBalancerDescriptions()) {

            setCanonicalHostedZoneNameId(lb.getCanonicalHostedZoneNameID());
            setDnsName(lb.getDNSName());
        }

        // Health check has to be configured separately.
        getHealthCheck().update(cloud, null, null);

        // Attributes.
        modifyAttributes(client);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, LoadBalancerDescription> current, Set<String> changedProperties) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        String name = getLoadBalancerName();
        LoadBalancerResource currentLbResource = (LoadBalancerResource) current;

        // Update subnet associations.
        Set<String> currentSubnetIds = awsIdSet(currentLbResource.getSubnets());
        Set<String> pendingSubnetIds = awsIdSet(getSubnets());
        Set<String> detachSubnetIds = new HashSet<>(currentSubnetIds);
        Set<String> attachSubnetIds = new HashSet<>(pendingSubnetIds);

        detachSubnetIds.removeAll(pendingSubnetIds);
        attachSubnetIds.removeAll(currentSubnetIds);

        if (!detachSubnetIds.isEmpty()) {
            DetachLoadBalancerFromSubnetsRequest dlbfsRequest = new DetachLoadBalancerFromSubnetsRequest();

            dlbfsRequest.setLoadBalancerName(name);
            dlbfsRequest.setSubnets(detachSubnetIds);
            client.detachLoadBalancerFromSubnets(dlbfsRequest);
        }

        if (!attachSubnetIds.isEmpty()) {
            AttachLoadBalancerToSubnetsRequest albtsRequest = new AttachLoadBalancerToSubnetsRequest();

            albtsRequest.setLoadBalancerName(name);
            albtsRequest.setSubnets(attachSubnetIds);
            client.attachLoadBalancerToSubnets(albtsRequest);
        }

        // Update security group associations.
        Set<String> securityGroupIds = awsIdSet(getSecurityGroups());

        if (!securityGroupIds.isEmpty()) {
            ApplySecurityGroupsToLoadBalancerRequest asgtlbRequest = new ApplySecurityGroupsToLoadBalancerRequest();

            asgtlbRequest.setLoadBalancerName(name);
            asgtlbRequest.setSecurityGroups(securityGroupIds);
            client.applySecurityGroupsToLoadBalancer(asgtlbRequest);
        }

        // Configure health check.
        getHealthCheck().update(cloud, null, null);

        // Adds any missing tags.
        Map<String, String> tags = getTags();

        if (!tags.isEmpty()) {
            AddTagsRequest atRequest = new AddTagsRequest();

            for (Map.Entry<String, String> entry : tags.entrySet()) {
                atRequest.getTags().add(new Tag().
                        withKey(entry.getKey()).
                        withValue(entry.getValue()));
            }

            atRequest.setLoadBalancerNames(Arrays.asList(getLoadBalancerName()));
            client.addTags(atRequest);
        }

        // Attributes.
        modifyAttributes(client);
    }

    private void modifyAttributes(AmazonElasticLoadBalancingClient client) {
        ModifyLoadBalancerAttributesRequest mlbaRequest = new ModifyLoadBalancerAttributesRequest();

        mlbaRequest.setLoadBalancerName(getLoadBalancerName());

        LoadBalancerAttributes attrs = new LoadBalancerAttributes();

        attrs.setCrossZoneLoadBalancing(new CrossZoneLoadBalancing().withEnabled(getCrossZoneLoadBalancing()));
        attrs.setConnectionSettings(new ConnectionSettings().withIdleTimeout(getIdleTimeout()));

        mlbaRequest.setLoadBalancerAttributes(attrs);
        client.modifyLoadBalancerAttributes(mlbaRequest);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        DeleteLoadBalancerRequest dlbRequest = new DeleteLoadBalancerRequest();

        dlbRequest.setLoadBalancerName(getLoadBalancerName());
        client.deleteLoadBalancer(dlbRequest);
    }

    @Override
    public String toDisplayString() {
        return "load balancer " + getLoadBalancerName();
    }
}
