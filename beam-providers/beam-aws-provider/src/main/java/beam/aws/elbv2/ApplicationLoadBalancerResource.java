package beam.aws.elbv2;

import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;

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
 *     aws::alb alb-example
 *         load-balancer-name: "alb-example"
 *         ip-address-type: "ipv4"
 *         scheme: "internal"
 *         security-groups: [
 *                 $(aws::security-group security-group | group-id)
 *             ]
 *         subnets: [
 *                 $(aws::subnet subnet-us-east-2a | subnet-id),
 *                 $(aws::subnet subnet-us-east-2b | subnet-id)
 *             ]
 *         tags: {
 *                 Name: "alb-example"
 *             }
 *     end
 */

@ResourceName("alb")
public class ApplicationLoadBalancerResource extends LoadBalancerResource {

    private List<String> securityGroups;
    private List<String> subnets;

    /**
     *  List of security groups associated with the alb (Optional)
     */
    public List<String> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new ArrayList<>();
        }

        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }

    /**
     *  List of subnets associated with the alb (Optional)
     */
    public List<String> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<String> subnets) {
        this.subnets = subnets;
    }

    @Override
    public boolean refresh() {
        LoadBalancer loadBalancer = super.internalRefresh();

        if (loadBalancer != null) {
            setSecurityGroups(loadBalancer.securityGroups());

            getSubnets().clear();
            for (AvailabilityZone az: loadBalancer.availabilityZones()) {
                getSubnets().add(az.subnetId());
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
                .securityGroups(getSecurityGroups())
                .subnets(getSubnets())
                .type(LoadBalancerTypeEnum.APPLICATION)
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
        sb.append("application load balancer " + getLoadBalancerName());
        return sb.toString();
    }
}
