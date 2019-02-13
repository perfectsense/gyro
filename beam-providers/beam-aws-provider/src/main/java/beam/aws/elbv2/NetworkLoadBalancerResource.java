package beam.aws.elbv2;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAddress;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SubnetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::nlb nlb-example
 *         load-balancer-name: "nlb-example"
 *         ip-address-type: "ipv4"
 *         scheme: "internet-facing"
 *
 *         subnet-mapping
 *             subnet-id: $(aws::subnet subnet-us-east-2a | subnet-id)
 *         end
 *
 *         subnet-mapping
 *             subnet-id: $(aws::subnet subnet-us-east-2b | subnet-id)
 *         end
 *
 *         tags: {
 *                 Name: "nlb-example"
 *             }
 *     end
 */

@ResourceName("nlb")
public class NetworkLoadBalancerResource extends LoadBalancerResource {

    private List<SubnetMappingResource> subnetMapping;

    /**
     * The list of subnet mappings associated with the nlb (Required)
     *
     * @subresource beam.aws.elbv2.SubnetMappingResource
     */
    @ResourceDiffProperty(subresource = true, nullable = true, updatable = true)
    public List<SubnetMappingResource> getSubnetMapping() {
        if (subnetMapping == null) {
            subnetMapping = new ArrayList<>();
        }

        return subnetMapping;
    }

    public void setSubnetMapping(List<SubnetMappingResource> subnetMapping) {
        this.subnetMapping = subnetMapping;
    }

    @Override
    public boolean refresh() {
        LoadBalancer loadBalancer = super.internalRefresh();

        if (loadBalancer != null) {

            getSubnetMapping().clear();
            for (AvailabilityZone zone : loadBalancer.availabilityZones()) {
                SubnetMappingResource subnet = new SubnetMappingResource();
                subnet.parent(this);
                subnet.setSubnetId(zone.subnetId());
                for (LoadBalancerAddress address : zone.loadBalancerAddresses()) {
                    subnet.setAllocationId(address.allocationId());
                    subnet.setIpAddress(address.ipAddress());
                }
                getSubnetMapping().add(subnet);
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        
        CreateLoadBalancerResponse response = client.createLoadBalancer(r -> r.ipAddressType(getIpAddressType())
                .name(getLoadBalancerName())
                .scheme(getScheme())
                .subnetMappings(toSubnetMappings())
                .type(LoadBalancerTypeEnum.NETWORK)
        );

        setLoadBalancerArn(response.loadBalancers().get(0).loadBalancerArn());
        setDnsName(response.loadBalancers().get(0).dnsName());

        super.create();
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        super.update(current, changedProperties);
    }

    @Override
    public void delete() {
        super.delete();
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("network load balancer " + getLoadBalancerName());
        return sb.toString();
    }

    private List<SubnetMapping> toSubnetMappings() {
        List<SubnetMapping> subnetMappings = new ArrayList<>();

        for (SubnetMappingResource subMap : getSubnetMapping()) {
            subnetMappings.add(subMap.toSubnetMapping());
        }

        return subnetMappings;
    }
}
