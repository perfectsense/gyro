package gyro.aws.dlm;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.dlm.DlmClient;
import software.amazon.awssdk.services.dlm.model.CreateLifecyclePolicyResponse;
import software.amazon.awssdk.services.dlm.model.CreateRule;
import software.amazon.awssdk.services.dlm.model.GetLifecyclePolicyResponse;
import software.amazon.awssdk.services.dlm.model.LifecyclePolicy;
import software.amazon.awssdk.services.dlm.model.PolicyDetails;
import software.amazon.awssdk.services.dlm.model.RetainRule;
import software.amazon.awssdk.services.dlm.model.Schedule;
import software.amazon.awssdk.services.dlm.model.Tag;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates an Auto scaling Group from a Launch Configuration or from a Launch Template.
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *     aws::ebs-snapshot-lifecycle-policy ebs-snapshot-lifecycle-policy-example
 *         description: "ebs-snapshot-lifecycle-policy-example"
 *         execution-role-arn: "arn:aws:iam::242040583208:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
 *         schedule-name: "ebs-snapshot-lifecycle-policy-example-schedule"
 *         retain-rule-count: 1000
 *         rule-time: "09:00"
 *         rule-interval: 6
 *         tags-to-add: {
 *             addTag: "tag1-val"
 *         }
 *         target-tags: {
 *             targetTag: "tag1-val"
 *         }
 *     end
 */
@ResourceName("ebs-snapshot-lifecycle-policy")
public class EbsSnapshotLifecyclePolicyResource extends AwsResource {

    private String policyId;
    private String description;
    private String executionRoleArn;
    private String resourceType;
    private Map<String, String> targetTags; // one is required
    private String state;
    private Date dateCreated;
    private Date dateModified;

    //----Schedule fields----//
    private Boolean copyTags;
    private String scheduleName;
    private Integer ruleInterval; //Valid intervals are: {2, 3, 4, 6, 8, 12, 24}
    private String ruleIntervalUnit; //HOURS
    private String ruleTime; //hh:mm
    private Integer retainRuleCount; // 1 to 1000
    private Map<String, String> tagsToAdd;

    @ResourceOutput
    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    /**
     * The description of the snapshot policy. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The permission role arn for teh snapshot policy. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getExecutionRoleArn() {
        return executionRoleArn;
    }

    public void setExecutionRoleArn(String executionRoleArn) {
        this.executionRoleArn = executionRoleArn;
    }

    /**
     * The description of the snapshot policy. Valid values ``VOLUME``. Defaults to ``VOLUME``
     */
    @ResourceDiffProperty(updatable = true)
    public String getResourceType() {
        if (resourceType == null) {
            resourceType = "VOLUME";
        }

        return resourceType.toUpperCase();
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * The target tags for the snapshot policy. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTargetTags() {
        if (targetTags == null) {
            targetTags = new HashMap<>();
        }

        return targetTags;
    }

    public void setTargetTags(Map<String, String> targetTags) {
        this.targetTags = targetTags;
    }

    /**
     * The state of the snapshot policy. Valid values ``ENABLED`` or ``DISABLED``. Defaults to ``ENABLED``
     */
    @ResourceDiffProperty(updatable = true)
    public String getState() {
        if (state == null) {
            state = "ENABLED";
        }

        return state.toUpperCase();
    }

    public void setState(String state) {
        this.state = state;
    }

    @ResourceOutput
    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @ResourceOutput
    public Date getDateModified() {
        return dateModified;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    //--Schedule-field-getter-setter--/

    /**
     * Copy tags to volumes created using this snapshot policy. Valid values ``true`` or ``false ``. Defaults to ``false``
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getCopyTags() {
        if (copyTags == null) {
            copyTags = false;
        }

        return copyTags;
    }

    public void setCopyTags(Boolean copyTags) {
        this.copyTags = copyTags;
    }

    /**
     * The name of the schedule for the snapshot policy. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    /**
     * The name of the schedule for the snapshot policy. Valid values ``2``,``3``,``4``,``6``,``8``, ``12`` and ``24``  (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getRuleInterval() {
        return ruleInterval;
    }

    public void setRuleInterval(Integer ruleInterval) {
        this.ruleInterval = ruleInterval;
    }

    /**
     * The rule interval for the snapshot policy. Valid values ``HOURS``. Defaults to ``HOURS``
     */
    @ResourceDiffProperty(updatable = true)
    public String getRuleIntervalUnit() {
        if (ruleIntervalUnit == null) {
            ruleIntervalUnit = "HOURS";
        }

        return ruleIntervalUnit.toUpperCase();
    }

    public void setRuleIntervalUnit(String ruleIntervalUnit) {
        this.ruleIntervalUnit = ruleIntervalUnit;
    }

    /**
     * The time format of the interval for the snapshot policy. Valid values ``hh:mm``. Defaults to ``hh:mm``
     */
    @ResourceDiffProperty(updatable = true)
    public String getRuleTime() {
        if (ruleTime == null) {
            ruleTime = "hh:mm";
        }

        return ruleTime;
    }

    public void setRuleTime(String ruleTime) {
        this.ruleTime = ruleTime;
    }

    /**
     * The number of volumes to retain for the snapshot policy. Valid values ``1`` to ``1000`` (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getRetainRuleCount() {
        return retainRuleCount;
    }

    public void setRetainRuleCount(Integer retainRuleCount) {
        this.retainRuleCount = retainRuleCount;
    }

    /**
     * The list of tags to add to the volumes for the snapshot policy. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTagsToAdd() {
        if (tagsToAdd == null) {
            tagsToAdd = new HashMap<>();
        }

        return tagsToAdd;
    }

    public void setTagsToAdd(Map<String, String> tagsToAdd) {
        this.tagsToAdd = tagsToAdd;
    }

    @Override
    public boolean refresh() {
        DlmClient client = createClient(DlmClient.class);

        LifecyclePolicy policy = getPolicy(client);
        setDateCreated(Date.from(policy.dateCreated()));
        setDateModified(Date.from(policy.dateModified()));
        setDescription(policy.description());
        setExecutionRoleArn(policy.executionRoleArn());
        setPolicyId(policy.policyId());
        setState(policy.stateAsString());

        PolicyDetails policyDetails = policy.policyDetails();
        setResourceType(policyDetails.resourceTypes().get(0).name());

        for (Schedule schedule : policyDetails.schedules()) {
            setCopyTags(schedule.copyTags());
            setScheduleName(schedule.name());

            for (Tag tag : schedule.tagsToAdd()) {
                getTagsToAdd().put(tag.key(), tag.value());
            }

            CreateRule createRule = schedule.createRule();
            if (createRule != null) {
                setRuleInterval(createRule.interval());
                setRuleIntervalUnit(createRule.intervalUnitAsString());
                setRuleTime(createRule.times().get(0));
            }

            RetainRule retainRule = schedule.retainRule();

            if (retainRule != null) {
                setRetainRuleCount(retainRule.count());
            }

            break;
        }

        getTargetTags().clear();
        for (Tag tag : policyDetails.targetTags()) {
            getTargetTags().put(tag.key(), tag.value());
        }

        return true;
    }

    @Override
    public void create() {
        DlmClient client = createClient(DlmClient.class);

        CreateLifecyclePolicyResponse response = client.createLifecyclePolicy(
            r -> r.description(getDescription())
                .executionRoleArn(getExecutionRoleArn())
                .policyDetails(
                    pd -> pd.resourceTypesWithStrings(Collections.singletonList(getResourceType()))
                        .schedules(Collections.singleton(getSchedule()))
                        .targetTags(getTags(getTargetTags()))
                )
                .state(getState())
        );

        setPolicyId(response.policyId());

        LifecyclePolicy policy = getPolicy(client);
        setDateCreated(Date.from(policy.dateCreated()));
        setDateModified(Date.from(policy.dateModified()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        DlmClient client = createClient(DlmClient.class);

        client.updateLifecyclePolicy(
            r -> r.policyId(getPolicyId())
                .description(getDescription())
                .executionRoleArn(getExecutionRoleArn())
                .policyDetails(
                    pd -> pd.resourceTypesWithStrings(Collections.singletonList(getResourceType()))
                        .schedules(Collections.singleton(getSchedule()))
                        .targetTags(getTags(getTargetTags()))
                )
                .state(getState())
        );
    }

    @Override
    public void delete() {
        DlmClient client = createClient(DlmClient.class);

        client.deleteLifecyclePolicy(
            r -> r.policyId(getPolicyId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ebs snapshot lifecycle");

        if (!ObjectUtils.isBlank(getPolicyId())) {
            sb.append(" - ").append(getPolicyId());
        }

        return sb.toString();
    }

    private Schedule getSchedule() {
        return Schedule.builder()
            .tagsToAdd(getTags(getTagsToAdd()))
            .name(getScheduleName())
            .retainRule(r -> r.count(getRetainRuleCount()))
            .copyTags(getCopyTags())
            .createRule(
                cr -> cr.times(Collections.singletonList(getRuleTime()))
                    .interval(getRuleInterval())
                    .intervalUnit(getRuleIntervalUnit())
            )
            .build();
    }

    private List<Tag> getTags(Map<String, String> tagMap) {
        return tagMap.entrySet().stream()
            .map(o -> Tag.builder().key(o.getKey()).value(o.getValue()).build())
            .collect(Collectors.toList());
    }

    private LifecyclePolicy getPolicy(DlmClient client) {
        if (ObjectUtils.isBlank(getPolicyId())) {
            throw new BeamException("policy-id is missing, unable to load ebs snapshot policy.");
        }

        GetLifecyclePolicyResponse response = client.getLifecyclePolicy(
            r -> r.policyId(getPolicyId())
        );

        return response.policy();
    }
}
