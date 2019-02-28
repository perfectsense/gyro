package gyro.aws.elbv2;

import gyro.aws.AwsResource;
import gyro.core.diff.Create;
import gyro.core.diff.Delete;
import gyro.core.diff.ResourceName;
import gyro.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;

import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: gyro
 *
 *         target
 *             id: $(aws::instance instance-us-east-2a | instance-id)
 *             port: "80"
 *         end
 *
 *         target
 *             id: $(aws::instance instance-us-east-2b | instance-id)
 *             port: "443"
 *         end
 */

@ResourceName(parent = "target-group", value = "target")
public class TargetResource extends AwsResource {

    private String availabilityZone;
    private String id;
    private Integer port;

    public TargetResource() {

    }

    public TargetResource(TargetDescription description) {
        setAvailabilityZone(description.availabilityZone());
        setId(description.id());
        setPort(description.port());
    }

    /**
     *   The availability zone from where the target receives traffic (Optional)
     */
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     *  The ID of the target (Required)
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     *  The port on which the target is listening (Required)
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String primaryKey() {
        return String.format("%s %d", getId(), getPort());
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

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.registerTargets(r -> r.targets(toTarget())
                                    .targetGroupArn(getTargetGroupArn()));

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {}

    @Override
    public void delete() {
        if (parentResource().change() instanceof Delete) {
            return;
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.deregisterTargets(r -> r.targets(toTarget())
                                        .targetGroupArn(getTargetGroupArn()));

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getId() != null) {
            sb.append("target " + getId());
        } else {
            sb.append("target - port: ");
            sb.append(getPort());
        }

        return sb.toString();
    }

    public TargetDescription toTarget() {
        return TargetDescription.builder()
                .availabilityZone(getAvailabilityZone())
                .id(getId())
                .port(getPort())
                .build();
    }

    public String getTargetGroupArn() {
        TargetGroupResource parent = (TargetGroupResource) parentResource();

        if (parent != null) {
            return parent.getTargetGroupArn();
        }

        return null;
    }
}
