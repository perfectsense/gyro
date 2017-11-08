package beam.openstack.config;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.rackspace.autoscale.v1.domain.LaunchConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.LoadBalancer;
import org.jclouds.rackspace.autoscale.v1.domain.Personality;

import java.util.*;

public class LaunchConfigurationResource extends OpenStackResource<LaunchConfiguration> {

    private String serverName;
    private Map<String, String> metadata;
    private String image;
    private String flavor;
    private String userData;
    private Set<BeamReference> loadBalancers;
    private List<BeamReference> networks;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public Set<BeamReference> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new HashSet<>();
        }

        return loadBalancers;
    }

    public void setLoadBalancers(Set<BeamReference> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public List<BeamReference> getNetworks() {
        if (networks == null) {
            networks = new ArrayList<>();
        }

        return networks;
    }

    public void setNetworks(List<BeamReference> networks) {
        this.networks = networks;
    }

    public LaunchConfiguration buildLaunchConfiguration() {
        List<LoadBalancer> loadBalancerIds = new ArrayList<>();
        for (BeamReference reference : getLoadBalancers()) {
            LoadBalancerResource loadBalancerResource = (LoadBalancerResource) reference.resolve();
            LoadBalancer loadBalancer = LoadBalancer.builder()
                    .id(loadBalancerResource.getLoadBalancerId())
                    .port(loadBalancerResource.getPort()).build();

            loadBalancerIds.add(loadBalancer);
        }

        Set<String> networkUUIDs = new HashSet<>();
        networkUUIDs.add(ServerResource.PUBLIC_NET);
        networkUUIDs.add(ServerResource.SERVICE_NET);

        for (BeamReference reference : getNetworks()) {
            NetworkResource network = (NetworkResource) reference.resolve();
            networkUUIDs.add(network.getNetworkId());
        }

        LaunchConfiguration launchConfig = LaunchConfiguration.builder()
                .loadBalancers(loadBalancerIds)
                .serverMetadata(getMetadata())
                .serverName(getServerName())
                .serverImageRef(getImage())
                .serverFlavorRef(getFlavor())
                .serverDiskConfig(Server.DISK_CONFIG_MANUAL)
                .networks(new ArrayList(networkUUIDs))
                .personalities(ImmutableList.of(
                        Personality.builder()
                                .path("/etc/beam/userdata.json")
                                .contents(getUserData())
                                .build()))
                .type(LaunchConfiguration.LaunchConfigurationType.LAUNCH_SERVER)
                .build();

        return launchConfig;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getServerName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, LaunchConfiguration launchConfiguration) {
        setServerName(launchConfiguration.getServerName());
        setImage(launchConfiguration.getServerImageRef());
        setFlavor(launchConfiguration.getServerFlavorRef());
        setMetadata(launchConfiguration.getServerMetadata());
        setUserData(BaseEncoding.base64().
                encode(ObjectUtils.toJson(getMetadata()).getBytes(StringUtils.UTF_8)));

        for (String networkName : launchConfiguration.getNetworks()) {
            NeutronApi neutronApi = cloud.createNeutronApi();
            NetworkApi networkApi = neutronApi.getNetworkApi(getRegion());

            Network n = networkApi.get(networkName);
            if (n != null) {
                NetworkResource networkResource = new NetworkResource();
                networkResource.setRegion(getRegion());
                networkResource.init(cloud, filter, n);

                getNetworks().add(newReference(networkResource));
            }
        }

        Set<String> loadBalancers = new HashSet<>();
        for (LoadBalancer loadBalancer : launchConfiguration.getLoadBalancers()) {
            loadBalancers.add(String.valueOf(loadBalancer.getId()));
        }
        setLoadBalancers(newReferenceSet(LoadBalancerResource.class, loadBalancers));
    }

    @Override
    public void create(OpenStackCloud cloud) {

    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, LaunchConfiguration> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        return "launch configuration " + getServerName();
    }
}
