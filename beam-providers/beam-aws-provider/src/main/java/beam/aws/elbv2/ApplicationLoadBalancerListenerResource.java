package beam.aws.elbv2;

import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;

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
 *     aws::alb-listener listener-example
 *         port: "80"
 *         protocol: "HTTPS"
 *         load-balancer-arn: $(aws::alb alb-example | load-balancer-arn)
 *         default-certificate: "arn:aws:acm:us-east-2:acct:certificate/certificate-arn"
 *
 *         default-action
 *             target-group-arn: $(aws::target-group target-group-example | target-group-arn)
 *             type: "forward"
 *         end
 *     end
 */

@ResourceName("alb-listener")
public class ApplicationLoadBalancerListenerResource extends ListenerResource {

    private List<ActionResource> defaultAction;

    /**
     *  List of default actions associated with the listener (Optional)
     */
    @ResourceDiffProperty(subresource = true, updatable = true)
    public List<ActionResource> getDefaultAction() {
        if (defaultAction == null) {
            defaultAction = new ArrayList<>();
        }

        return defaultAction;
    }

    public void setDefaultAction(List<ActionResource> defaultAction) {
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

        if (toCertificates().isEmpty() && getProtocol().equals("HTTP")) {
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
            sb.append("alb listener " + getListenerArn());
        } else {
            sb.append("alb listener ");
        }
        return sb.toString();
    }

    private List<Action> toDefaultActions() {
        List<Action> defaultAction = new ArrayList<>();

        for (ActionResource resource : getDefaultAction()) {
            defaultAction.add(resource.toAction());
        }

        return defaultAction;
    }

    private List<ActionResource> fromDefaultActions(List<Action> actionList) {
        List<ActionResource> actions = new ArrayList<>();

        for (Action action : actionList) {
            ActionResource actionResource = new ActionResource(action);
            actionResource.parent(this);
            actions.add(actionResource);
        }

        return actions;
    }

    public void createDefaultAction(ActionResource defaultAction) {
        if (!getDefaultAction().contains(defaultAction)) {
            getDefaultAction().add(defaultAction);
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyListener(r -> r.certificates(toCertificates())
                .defaultActions(toDefaultActions())
                .listenerArn(getListenerArn())
                .port(getPort())
                .protocol(getProtocol())
                .sslPolicy(getSslPolicy()));

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

    public void deleteDefaultAction(ActionResource defaultAction) {
        if (getDefaultAction().contains(defaultAction)) {
            getDefaultAction().remove(defaultAction);
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyListener(r -> r.certificates(toCertificates())
                .defaultActions(toDefaultActions())
                .listenerArn(getListenerArn())
                .port(getPort())
                .protocol(getProtocol())
                .sslPolicy(getSslPolicy()));
    }
}
