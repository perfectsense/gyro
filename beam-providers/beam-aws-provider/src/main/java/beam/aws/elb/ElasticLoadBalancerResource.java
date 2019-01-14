package beam.aws.elb;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;

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
 *     aws::elastic-load-balancer elb-example
 *
 *     end
 */
@ResourceName("elastic-load-balancer")
public class ElasticLoadBalancerResource extends AwsResource {

    private String loadBalancerName;
    private List<ListenerResource> listeners;
    private List<String> securityGroups;
    private List<String> subnets;

    public List<ListenerResource> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        return listeners;
    }

    public List<Listener> toListeners() {
        List<Listener> listeners = new ArrayList<>();
        for(ListenerResource resource : getListeners()) {
            Listener newListener = Listener.builder()
                    .instancePort(resource.getInstancePort())
                    .instanceProtocol(resource.getInstanceProtocol())
                    .loadBalancerPort(resource.getLoadBalancerPort())
                    .protocol(resource.getProtocol())
                    .sslCertificateId(resource.getSslCertificateId())
                    .build();
            listeners.add(newListener);
        }

        return listeners;
    }

    public void setListeners(List<ListenerResource> listeners) {
        this.listeners = listeners;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    public List<String> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new ArrayList<>();
        }

        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }

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
    public boolean refresh() {return false;}

    @Override
    public void create() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .build();

       client.createLoadBalancer(r -> r.listeners(toListeners())
                .securityGroups(getSecurityGroups())
                .subnets(getSubnets())
        );

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {}

    @Override
    public void delete() {}

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }


}
