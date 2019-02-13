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

    private List<SubnetMappings> subnetMappings;

    /**
     *  The list of subnet mappings associated with the nlb (Required)
     */
    //@ResourceDiffProperty(subresource = true, nullable = true, updatable = true)
    public List<SubnetMappings> getSubnetMappings() {
        if (subnetMappings == null) {
            subnetMappings = new ArrayList<>();
        }

        return subnetMappings;
    }

    public void setSubnetMappings(List<SubnetMappings> subnetMappings) {
        this.subnetMappings = subnetMappings;
    }

    @Override
    public boolean refresh() {
        LoadBalancer loadBalancer = super.internalRefresh();

        if (loadBalancer != null) {

            getSubnetMappings().clear();
            for (AvailabilityZone zone : loadBalancer.availabilityZones()) {
                SubnetMappings subnet = new SubnetMappings();
                subnet.parent(this);
                subnet.setSubnetId(zone.subnetId());
                for (LoadBalancerAddress address : zone.loadBalancerAddresses()) {
                    subnet.setAllocationId(address.allocationId());
                    subnet.setIpAddress(address.ipAddress());
                }
                getSubnetMappings().add(subnet);
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

        for (SubnetMappings subMap : getSubnetMappings()) {
            subnetMappings.add(subMap.toSubnetMappings());
        }

        return subnetMappings;
    }
}
