package gyro.aws.elbv2;

import gyro.aws.AwsResource;
import gyro.core.diff.Create;
import gyro.core.diff.Delete;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.Update;
import gyro.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     condition
 *         field: "host-header"
 *         value: ["www.example.net"]
 *     end
 */

@ResourceName(parent = "alb-listener-rule", value = "condition")
public class ConditionResource extends AwsResource {

    private String field;
    private List<String> value;

    public ConditionResource() {

    }

    public ConditionResource(RuleCondition ruleCondition) {
        setField(ruleCondition.field());
        setValue(ruleCondition.values());
    }

    /**
     *  Condition field name  (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    /**
     *  Condition value (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getValue() {
        if (value == null) {
            value = new ArrayList<>();
        }

        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    @Override
    public String primaryKey() {
        return String.format("%s", getField(), getValue());
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

        ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
        parent.createCondition(this);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        if (parentResource().change() instanceof Update) {
            return;
        }

        ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
        parent.updateCondition();
    }

    @Override
    public void delete() {
        if (parentResource().change() instanceof Delete) {
            return;
        }

        ApplicationLoadBalancerListenerRuleResource parent = (ApplicationLoadBalancerListenerRuleResource) parentResource();
        parent.deleteCondition(this);
    }

    @Override
    public String toDisplayString() {
        return "rule condition - field: " + getField();
    }

    public RuleCondition toCondition() {
        return RuleCondition.builder()
                .field(getField())
                .values(getValue())
                .build();
    }
}
