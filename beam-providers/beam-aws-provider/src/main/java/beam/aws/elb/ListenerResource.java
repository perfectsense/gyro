package beam.aws.elb;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
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
 *     aws::listener listener-ex
 *        instance-port: "443"
 *        instance-protocol: "https"
 *        load-balancer-port: "443"
 *        protocol: "https"
 *        ssl-certificate-id: "arn:aws:iam::123456789012:server-certificate/my-server-cert"
 *     end
 */

@ResourceName(parent = "load-balancer-resource", value = "listener")
public class ListenerResource extends AwsResource {

    private Integer instancePort;
    private String instanceProtocol;
    private Integer loadBalancerPort;
    private String protocol;
    private String sslCertificateId;

    public Integer getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(Integer instancePort) { this.instancePort = instancePort; }

    public String getInstanceProtocol() {
        return instanceProtocol;
    }

    public void setInstanceProtocol(String instanceProtocol) { this.instanceProtocol = instanceProtocol; }

    public Integer getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(Integer loadBalancerPort) { this.loadBalancerPort = loadBalancerPort; }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getSslCertificateId() { return sslCertificateId; }

    public void setSslCertificateId(String sslCertificateId) { this.sslCertificateId = sslCertificateId; }

    public String getLoadBalancer() {
        LoadBalancerResource parent = (LoadBalancerResource) parentResourceNode();
        if (parent != null) {
            return parent.getLoadBalancerName();
        }

        return null;
    }

    @Override
    public boolean refresh() {return false;}

    @Override
    public void create() {}

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        //remove and attach based on new list of listeners
        LoadBalancerResource parent = (LoadBalancerResource) parentResourceNode();
        //parent.update();

    }

    @Override
    public void delete() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .build();

        client.deleteLoadBalancerListeners(r -> r.loadBalancerName(getLoadBalancer()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }
}
