package beam.aws.elb;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;

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
 *
 *
 *     end
 */

@ResourceName(parent = "elastic-load-balancer", value = "listener")
public class ListenerResource extends ElasticLoadBalancerResource {
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

    public void setInstancePort(String instanceProtocol) { this.instanceProtocol = instanceProtocol; }

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

    @Override
    public boolean refresh() {return false;}

    @Override
    public void create() {}

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
