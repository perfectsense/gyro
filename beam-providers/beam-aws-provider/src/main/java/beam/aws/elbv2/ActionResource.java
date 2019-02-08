package beam.aws.elbv2;

import beam.core.diff.Create;
import beam.core.diff.Delete;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.core.diff.Update;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AuthenticateCognitoActionConfig;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AuthenticateOidcActionConfig;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.FixedResponseActionConfig;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RedirectActionConfig;

import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     action
 *         target-group-arn: $(aws::target-group target-group-example | target-group-arn)
 *         type: "forward"
 *     end
 */

@ResourceName(parent = "alb-listener", value = "default-action")
@ResourceName(parent = "alb-listener-rule", value = "action")
public class ActionResource extends NetworkActionResource {

    private AuthenticateCognitoActionConfigResource cognitoAction;
    private AuthenticateOidcActionConfigResource oidc;
    private FixedResponseActionConfigResource fixedResponse;
    private Integer order;
    private RedirectActionConfigResource redirect;
    private String targetGroupArn;
    private String type;

    public ActionResource() {

    }

    public ActionResource(Action action) {

        AuthenticateCognitoActionConfig cognitoConfig = action.authenticateCognitoConfig();
        if (cognitoConfig != null) {
            AuthenticateCognitoActionConfigResource cognito = new AuthenticateCognitoActionConfigResource(cognitoConfig);
            setCognitoAction(cognito);
        }

        AuthenticateOidcActionConfig oidcConfig = action.authenticateOidcConfig();
        if (oidcConfig != null) {
            AuthenticateOidcActionConfigResource oidc = new AuthenticateOidcActionConfigResource(oidcConfig);
            setOidc(oidc);
        }

        FixedResponseActionConfig fixedConfig = action.fixedResponseConfig();
        if (fixedConfig != null) {
            FixedResponseActionConfigResource fixed = new FixedResponseActionConfigResource(fixedConfig);
            setFixedResponse(fixed);
        }

        RedirectActionConfig redirectConfig = action.redirectConfig();
        if (redirectConfig != null) {
            RedirectActionConfigResource redirect = new RedirectActionConfigResource(redirectConfig);
            setRedirect(redirect);
        }

        setOrder(action.order());
        setTargetGroupArn(action.targetGroupArn());
        setType(action.typeAsString());
    }

    /**
     *  Authentication through user pools supported by Amazon Cognito (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public AuthenticateCognitoActionConfigResource getCognitoAction() {
        return cognitoAction;
    }

    public void setCognitoAction(AuthenticateCognitoActionConfigResource cognitoAction) {
        this.cognitoAction = cognitoAction;
    }

    /**
     *  Authentication through provider that is OpenID Connect (OIDC) compliant (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public AuthenticateOidcActionConfigResource getOidc() {
        return oidc;
    }

    public void setOidc(AuthenticateOidcActionConfigResource oidc) {
        this.oidc = oidc;
    }

    /**
     *  Used to specify a custom response for an action  (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public FixedResponseActionConfigResource getFixedResponse() {
        return fixedResponse;
    }

    public void setFixedResponse(FixedResponseActionConfigResource fixedResponse) {
        this.fixedResponse = fixedResponse;
    }

    /**
     *  The order in which the action should take place (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     *  Redirect requests from one URL to another (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public RedirectActionConfigResource getRedirect() {
        return redirect;
    }

    public void setRedirect(RedirectActionConfigResource redirect) {
        this.redirect = redirect;
    }

    /**
     *  The target group arn that this action is associated with  (Optional)
     */
    public String getTargetGroupArn() {
        return targetGroupArn;
    }

    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }

    /**
     *  The type of action to perform  (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String primaryKey() {
        return String.format("%d %s", getOrder(), getType());
    }

    @Override
    public boolean refresh() {
        return true;
    }

    @Override
    public void create() {
        if (parentResource().change() instanceof Create) {
            return;
        }

        if (parentResource() instanceof ApplicationLoadBalancerListenerRuleResource) {
            ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
            parent.createAction(this);
        } else {
            ApplicationLoadBalancerListenerResource parent = (ApplicationLoadBalancerListenerResource) parentResource();
            parent.createDefaultAction(this);
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        if (parentResource().change() instanceof Update) {
            return;
        }

        if (parentResource() instanceof ApplicationLoadBalancerListenerRuleResource) {
            ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
            parent.updateAction();
        } else {
            ApplicationLoadBalancerListenerResource parent = (ApplicationLoadBalancerListenerResource) parentResource();
            parent.updateDefaultAction();
        }
    }

    @Override
    public void delete() {
        if (parentResource().change() instanceof Delete) {
            return;
        }

        if (parentResource() instanceof ApplicationLoadBalancerListenerRuleResource) {
            ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
            parent.deleteAction(this);
        } else {
            ApplicationLoadBalancerListenerResource parent = (ApplicationLoadBalancerListenerResource) parentResource();
            parent.deleteDefaultAction(this);
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (parentResource() instanceof ListenerResource) {
            sb.append("listener default action");
        } else {
            sb.append("listener rule action - type:" + getType());
        }

        return sb.toString();
    }

    public Action toAction() {
        return Action.builder()
                .authenticateCognitoConfig(getCognitoAction() != null ? getCognitoAction().toCognito() : null)
                .authenticateOidcConfig(getOidc() != null ? getOidc().toOidc() : null)
                .fixedResponseConfig(getFixedResponse() != null ? getFixedResponse().toFixedAction() : null)
                .redirectConfig(getRedirect() != null ? getRedirect().toRedirect() : null)
                .order(getOrder())
                .targetGroupArn(getTargetGroupArn())
                .type(getType())
                .build();
    }
}


