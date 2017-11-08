package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateOrUpdateTagsRequest;
import com.amazonaws.services.autoscaling.model.DeleteTagsRequest;
import com.amazonaws.services.autoscaling.model.Tag;

public class AutoScalingGroupTagResource extends AWSResource<Tag> {

    private BeamReference autoScalingGroup;
    private String key;
    private Boolean propagateAtLaunch;
    private String value;

    public BeamReference getAutoScalingGroup() {
        return newParentReference(AutoScalingGroupResource.class, autoScalingGroup);
    }

    public void setAutoScalingGroup(BeamReference autoScalingGroup) {
        this.autoScalingGroup = autoScalingGroup;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getPropagateAtLaunch() {
        return propagateAtLaunch;
    }

    public void setPropagateAtLaunch(Boolean propagateAtLaunch) {
        this.propagateAtLaunch = propagateAtLaunch;
    }

    @ResourceDiffProperty(updatable = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Tag toTag() {
        return new Tag().
                withKey(getKey()).
                withPropagateAtLaunch(getPropagateAtLaunch()).
                withResourceId(getAutoScalingGroup().awsId()).
                withResourceType("auto-scaling-group").
                withValue(getValue());
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getAutoScalingGroup(), getKey());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, Tag tag) {
        setKey(tag.getKey());
        setPropagateAtLaunch(tag.getPropagateAtLaunch());
        setValue(tag.getValue());
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        CreateOrUpdateTagsRequest coutRequest = new CreateOrUpdateTagsRequest();

        coutRequest.getTags().add(toTag());
        client.createOrUpdateTags(coutRequest);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Tag> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonAutoScalingClient client = createClient(AmazonAutoScalingClient.class, cloud.getProvider());
        DeleteTagsRequest dtRequest = new DeleteTagsRequest();

        dtRequest.getTags().add(toTag());
        client.deleteTags(dtRequest);
    }

    @Override
    public String toDisplayString() {
        return "auto scaling tag " + getKey();
    }
}
