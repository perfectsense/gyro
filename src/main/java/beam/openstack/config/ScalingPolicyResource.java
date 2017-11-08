package beam.openstack.config;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.domain.ScalingPolicy;

import java.util.List;
import java.util.Set;

public class ScalingPolicyResource extends OpenStackResource<ScalingPolicy> {

    private String name;
    private int coolDown;
    private String targetType;
    private String target;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public CreateScalingPolicy buildScalingPolicy() {
        CreateScalingPolicy scalingPolicy = CreateScalingPolicy.builder()
                .cooldown(0)
                .type(CreateScalingPolicy.ScalingPolicyType.WEBHOOK)
                .name(getName())
                .targetType(CreateScalingPolicy.ScalingPolicyTargetType.INCREMENTAL)
                .target(getTarget())
                .build();

        return scalingPolicy;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, ScalingPolicy policy) {
        setName(policy.getName());
        setTarget(policy.getTarget());
        setTargetType(policy.getTargetType().toString());

    }

    @Override
    public void create(OpenStackCloud cloud) {

    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, ScalingPolicy> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public String toDisplayString() {
        return "auto scaling group policy " + getName();
    }
}
