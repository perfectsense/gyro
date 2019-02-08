package beam.aws.elbv2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateRuleResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeRulesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleNotFoundException;

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
 *     aws::alb-listener-rule listener-rule-example
 *         listener-arn: $(aws::alb-listener listener-example | listener-arn)
 *         priority: "1"
 *
 *         action
 *             target-group-arn: $(aws::target-group target-group-example | target-group-arn)
 *             type: "forward"
 *         end
 *
 *         condition
 *             field: "path-pattern"
 *             value: ["/applespice"]
 *         end
 *
 *         condition
 *             field: "host-header"
 *             value: ["www.example.net"]
 *         end
 *     end
 */

@ResourceName("alb-listener-rule")
public class ApplicationLoadBalancerListenerRuleResource extends AwsResource {

    private List<ActionResource> action;
    private List<ConditionResource> condition;
    private String listenerArn;
    private Integer priority;
    private String ruleArn;

    /**
     *  List of actions associated with the rule (Required)
     */
    @ResourceDiffProperty(updatable = true, subresource = true)
    public List<ActionResource> getAction() {
        if (action == null) {
            action = new ArrayList<>();
        }

        return action;
    }

    public void setAction(List<ActionResource> action) {
        this.action = action;
    }

    /**
     *  List of conditions associated with the rule (Required)
     */
    @ResourceDiffProperty(updatable = true, subresource = true)
    public List<ConditionResource> getCondition() {
        if (condition == null) {
            condition = new ArrayList<>();
        }

        return condition;
    }

    public void setCondition(List<ConditionResource> condition) {
        this.condition = condition;
    }

    public String getListenerArn() {
        return listenerArn;
    }

    public void setListenerArn(String listenerArn) {
        this.listenerArn = listenerArn;
    }

    /**
     *  Priority of the rule (Required)
     */
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getRuleArn() {
        return ruleArn;
    }

    public void setRuleArn(String ruleArn) {
        this.ruleArn = ruleArn;
    }

    @Override
    public boolean refresh() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        try {
            DescribeRulesResponse response = client.describeRules(r -> r.ruleArns(getRuleArn()));

            Rule rule = response.rules().get(0);
            setAction(fromActions(rule.actions()));
            setCondition(fromCondition(rule.conditions()));
            setPriority(Integer.valueOf(rule.priority()));
            setRuleArn(rule.ruleArn());

            return true;

        } catch (RuleNotFoundException ex) {
            return false;
        }
    }

    @Override
    public void create() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        CreateRuleResponse response = client.createRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .listenerArn(getListenerArn())
                .priority(getPriority()));

        setRuleArn(response.rules().get(0).ruleArn());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                                .conditions(toConditions())
                                .ruleArn(getRuleArn()));
    }

    @Override
    public void delete() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.deleteRule(r -> r.ruleArn(getRuleArn()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getRuleArn() != null) {
            sb.append("alb listener rule " + getRuleArn());
        } else {
            sb.append("alb listener rule");
        }

        return sb.toString();
    }

    private List<Action> toActions() {
        List<Action> actionsList = new ArrayList<>();

        for (ActionResource act : getAction()) {
            actionsList.add(act.toAction());
        }

        return actionsList;
    }

    private List<RuleCondition> toConditions() {
        List<RuleCondition> conditionList = new ArrayList<>();

        for (ConditionResource ruleCondition : getCondition()) {
            conditionList.add(ruleCondition.toCondition());
        }

        return conditionList;
    }

    private List<ActionResource> fromActions(List<Action> actionList) {
        List<ActionResource> actions = new ArrayList<>();

        for (Action action : actionList) {
            ActionResource actionResource = new ActionResource(action);
            actionResource.parent(this);
            actions.add(actionResource);
        }
        return actions;
    }

    private List<ConditionResource> fromCondition(List<RuleCondition> conditionsList) {
        List<ConditionResource> conditions = new ArrayList<>();

        for (RuleCondition rc : conditionsList) {
            ConditionResource condition = new ConditionResource(rc);
            condition.parent(this);
            conditions.add(condition);
        }

        return conditions;
    }

    public void createAction(ActionResource action) {
        if (!getAction().contains(action)) {
            getAction().add(action);
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }

    public void updateAction() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }

    public void deleteAction(ActionResource action) {
        getAction().remove(action);

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }

    public void createCondition(ConditionResource condition) {
        if (!getCondition().contains(condition)) {
            getCondition().add(condition);
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }

    public void updateCondition() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }

    public void deleteCondition(ConditionResource condition) {
        getCondition().remove(condition);

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.modifyRule(r -> r.actions(toActions())
                .conditions(toConditions())
                .ruleArn(getRuleArn()));
    }
}
