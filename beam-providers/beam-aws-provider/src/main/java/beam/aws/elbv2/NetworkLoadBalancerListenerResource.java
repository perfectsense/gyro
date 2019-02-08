package beam.aws.elbv2;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;

import java.util.List;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::nlb-listener listener-example
 *         port: "80"
 *         protocol: "TCP"
 *         load-balancer-arn: $(aws::nlb nlb-example | load-balancer-arn)
 *
 *         default-action
 *             target-group-arn: $(aws::target-group target-group-example | target-group-arn)
 *             type: "forward"
 *         end
 *     end
 */

@ResourceName("nlb-listener")
public class NetworkLoadBalancerListenerResource extends ListenerResource {

    private NetworkActionResource defaultAction;

    /**
     *  List of default actions associated with the listener (Optional)
     */
    @ResourceDiffProperty(subresource = true, updatable = true)
    public NetworkActionResource getDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(NetworkActionResource defaultAction) {
        this.defaultAction = defaultAction;
    }

    @Override
    public boolean refresh() {
        Listener listener = super.internalRefresh();

        if (listener != null) {
            setDefaultAction(fromDefaultActions(listener.defaultActions()));
            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        CreateListenerResponse response =
                client.createListener(r -> r.certificates(Certificate.builder().certificateArn(getDefaultCertificate()).build())
                        .defaultActions(toDefaultActions())
                        .loadBalancerArn(getLoadBalancerArn())
                        .port(getPort())
                        .protocol(getProtocol())
                        .sslPolicy(getSslPolicy()));

        setListenerArn(response.listeners().get(0).listenerArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        if (getDefaultCertificate() == null && getProtocol().equals("TCP")) {
            client.modifyListener(r -> r.certificates(Certificate.builder().certificateArn(getDefaultCertificate()).build())
                    .defaultActions(toDefaultActions())
                    .listenerArn(getListenerArn())
                    .port(getPort())
                    .protocol(getProtocol())
                    .sslPolicy(null));
        } else {

            client.modifyListener(r -> r.certificates(Certificate.builder().certificateArn(getDefaultCertificate()).build())
                    .defaultActions(toDefaultActions())
                    .listenerArn(getListenerArn())
                    .port(getPort())
                    .protocol(getProtocol())
                    .sslPolicy(getSslPolicy()));
        }
    }

    @Override
    public void delete() {
        super.delete();
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getListenerArn() != null) {
            sb.append("nlb listener " + getListenerArn());
        } else {
            sb.append("nlb listener ");
        }
        return sb.toString();
    }

    private Action toDefaultActions() {
        return Action.builder()
                .type(defaultAction.getType())
                .targetGroupArn(defaultAction.getTargetGroupArn())
                .build();
    }

    private NetworkActionResource fromDefaultActions(List<Action> defaultAction) {
        NetworkActionResource actionResource = new NetworkActionResource();

        for (Action action : defaultAction) {
            actionResource.setTargetGroupArn(action.targetGroupArn());
            actionResource.setType(action.typeAsString());
            actionResource.parent(this);
        }

        return actionResource;
    }

    public void updateDefaultAction() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyListener(r -> r.certificates(toCertificates())
                .defaultActions(toDefaultActions())
                .listenerArn(getListenerArn())
                .port(getPort())
                .protocol(getProtocol())
                .sslPolicy(getSslPolicy()));
    }
}
