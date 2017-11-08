package beam.openstack.config;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceChange;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RegionResource extends OpenStackResource<Void> {

    private List<KeyPairResource> keyPairs;
    private List<SwiftContainerResource> containers;
    private List<LoadBalancerResource> loadBalancers;
    private List<NetworkResource> subnets;
    private List<GatewayRouteResource> gatewayRoutes;

    public String getName() {
        return getRegion();
    }

    public void setName(String name) {
        setRegion(name.toUpperCase());
    }

    public List<KeyPairResource> getKeyPairs() {
        if (keyPairs == null) {
            keyPairs = new ArrayList<>();
        }

        return keyPairs;
    }

    public void setKeyPairs(List<KeyPairResource> keyPairs) {
        this.keyPairs = keyPairs;
    }

    public List<SwiftContainerResource> getContainers() {
        if (containers == null) {
            containers = new ArrayList<>();
        }

        return containers;
    }

    public void setContainers(List<SwiftContainerResource> containers) {
        this.containers = containers;
    }

    public List<LoadBalancerResource> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new ArrayList<>();
        }

        return loadBalancers;
    }

    public void setLoadBalancers(List<LoadBalancerResource> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public List<NetworkResource> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<NetworkResource> subnets) {
        this.subnets = subnets;
    }

    public List<GatewayRouteResource> getGatewayRoutes() {
        if (gatewayRoutes == null) {
            gatewayRoutes = new ArrayList<>();
        }

        return gatewayRoutes;
    }

    public void setGatewayRoutes(List<GatewayRouteResource> gatewayRoutes) {
        this.gatewayRoutes = gatewayRoutes;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Void unused) {
        NeutronApi neutronApi = cloud.createNeutronApi();
        NetworkApi networkApi = neutronApi.getNetworkApi(getRegion());

        for (Network network : networkApi.list().concat()) {
            if (filter.isInclude(network)) {
                NetworkResource networkResource = new NetworkResource();
                networkResource.setRegion(getName());
                networkResource.init(cloud, filter, network);

                getSubnets().add(networkResource);

                if (networkResource.getGateway() != null) {
                    GatewayRouteResource gatewayRouteResource = new GatewayRouteResource();
                    gatewayRouteResource.setGateway(newReference(networkResource.getGateway()));
                    gatewayRouteResource.setNetwork(newReference(networkResource));

                    getGatewayRoutes().add(gatewayRouteResource);
                }
            }
        }

        // Key pairs.
        KeyPairApi keyPairApi = cloud.createApi().getKeyPairApi(getRegion()).get();
        for (KeyPair keypair : keyPairApi.list()) {
            if (filter.isInclude(keypair)) {
                KeyPairResource kpResource = new KeyPairResource();
                kpResource.setRegion(getName());
                kpResource.init(cloud, filter, keypair);

                getKeyPairs().add(kpResource);
            }
        }

        // Cloud Files containers.
        CloudFilesApi cloudFilesApi = cloud.createCloudFilesApi();
        ContainerApi containerApi = cloudFilesApi.getContainerApi(getName());
        for (Container container : containerApi.list()) {
            if (filter.isInclude(container)) {
                SwiftContainerResource containerResource = new SwiftContainerResource();
                containerResource.setRegion(getName());
                containerResource.init(cloud, filter, container);

                getContainers().add(containerResource);
            }
        }

        // Cloud Load Balancers.
        CloudLoadBalancersApi api = cloud.createCloudLoadBalancersApi();
        LoadBalancerApi lbApi = api.getLoadBalancerApi(getRegion());

        for (LoadBalancer loadBalancer : lbApi.list().concat()) {
            if (filter.isInclude(loadBalancer)) {
                LoadBalancerResource loadBalancerResource = new LoadBalancerResource();
                loadBalancerResource.setRegion(getRegion());
                loadBalancerResource.init(cloud, filter, loadBalancer);

                getLoadBalancers().add(loadBalancerResource);
            }
        }
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getKeyPairs());
        create.create(getGatewayRoutes());
        create.create(getSubnets());
        create.create(getContainers());
        create.create(getLoadBalancers());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Void> current) throws Exception {
        RegionResource currentRegion = (RegionResource) current;


        update.update(currentRegion.getKeyPairs(), getKeyPairs());
        update.update(currentRegion.getGatewayRoutes(), getGatewayRoutes());
        update.update(currentRegion.getSubnets(), getSubnets());
        update.update(currentRegion.getContainers(), getContainers());
        update.update(currentRegion.getLoadBalancers(), getLoadBalancers());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getKeyPairs());
        delete.delete(getGatewayRoutes());
        delete.delete(getSubnets());
        delete.delete(getContainers());
        delete.delete(getLoadBalancers());
    }

    @Override
    public void create(OpenStackCloud cloud) {

    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Void> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public String toDisplayString() {
        return "region " + getName();
    }
}
