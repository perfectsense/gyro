package beam.aws.elb;

import beam.aws.AwsResource;
import beam.core.diff.ChangeType;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.Instance;
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
 *     listener
 *        instance-port: "443"
 *        instance-protocol: "https"
 *        load-balancer-port: "443"
 *        protocol: "https"
 *        ssl-certificate-id: "arn:aws:iam::123456789012:server-certificate/my-server-cert"
 *     end
 */

@ResourceName(parent = "load-balancer", value = "listener")
public class ListenerResource extends AwsResource {

    private Integer instancePort;
    private String instanceProtocol;
    private Integer loadBalancerPort;
    private String protocol;
    private String sslCertificateId;

    @ResourceDiffProperty(updatable = true)
    public Integer getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(Integer instancePort) { this.instancePort = instancePort; }

    @ResourceDiffProperty(updatable = true)
    public String getInstanceProtocol() {
        return instanceProtocol;
    }

    public void setInstanceProtocol(String instanceProtocol) { this.instanceProtocol = instanceProtocol; }

    @ResourceDiffProperty(updatable = true)
    public Integer getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(Integer loadBalancerPort) { this.loadBalancerPort = loadBalancerPort; }

    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) { this.protocol = protocol; }
    /*
    @ResourceDiffProperty(updatable = true)
    public String getSslCertificateId() { return sslCertificateId; }

    public void setSslCertificateId(String sslCertificateId) { this.sslCertificateId = sslCertificateId; }
    */
    public String getLoadBalancer() {
        LoadBalancerResource parent = (LoadBalancerResource) parentResource();
        System.out.println("Is parent null "+(parent == null));

        if (parent != null) {
            System.out.println("Load balancer name "+parent.getLoadBalancerName());
            return parent.getLoadBalancerName();
        }

        return null;
    }

    public Listener toListener() {
            Listener newListener = Listener.builder()
                    .instancePort(getInstancePort())
                    .instanceProtocol(getInstanceProtocol())
                    .loadBalancerPort(getLoadBalancerPort())
                    .protocol(getProtocol())
                    //.sslCertificateId(resource.getSslCertificateId())
                    .build();

        return newListener;
    }


    @Override
    public boolean refresh() {
        return true;
    }

    @Override
    public void create() {
        if (parentResource().change().getType() == ChangeType.CREATE) {
            return;
        }

        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);
        client.createLoadBalancerListeners(r -> r.listeners(toListener()));

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        // parent must refresh because of the listeners list
        // call parent's update function
        // delete and create
        delete();
        create();
    }

    @Override
    public void delete() {
        // if parent is going to be deleted, then the listeners will
        // get deleted

        if (parentResource().change().getType() == ChangeType.DELETE) {
            return;
        }
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);

        System.out.println("Get load balancer name "+getLoadBalancer());
        client.deleteLoadBalancerListeners(r -> r.loadBalancerName(getLoadBalancer())
                                                .loadBalancerPorts(getLoadBalancerPort()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("listener");
        return sb.toString();
    }
}
