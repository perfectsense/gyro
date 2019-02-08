package beam.aws.elbv2;

import beam.aws.AwsResource;
import beam.core.diff.Create;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.core.diff.Update;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;

import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     default-action
 *        target-group-arn: $(aws::target-group target-group-example | target-group-arn)
 *        type: "forward"
 *     end
 */

@ResourceName(parent = "nlb-listener", value = "default-action")
public class NetworkActionResource extends AwsResource {

    private String targetGroupArn;
    private String type;

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
        return String.format("%s %s", getTargetGroupArn(), getType());
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

        NetworkLoadBalancerListenerResource parent = (NetworkLoadBalancerListenerResource) parentResource();
        parent.updateDefaultAction();
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        if (parentResource().change() instanceof Update) {
            return;
        }

        NetworkLoadBalancerListenerResource parent = (NetworkLoadBalancerListenerResource) parentResource();
        parent.updateDefaultAction();
    }

    @Override
    public void delete() {}

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nlb listener default action");
        return sb.toString();
    }

    public Action toAction() {
        return Action.builder()
                .targetGroupArn(getTargetGroupArn())
                .type(getType())
                .build();
    }
}
