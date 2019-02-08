package beam.aws.elbv2;

import beam.aws.AwsResource;
import beam.core.diff.Create;
import beam.core.diff.Delete;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SubnetMapping;

import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     subnet-mapping
 *         allocation-id: $(aws::elastic-ip elastic-ip-example | allocation-id)
 *         ip-address: $(aws::elastic-ip elastic-ip-example | public-ip)
 *         subnet-id: $(aws::subnet subnet-example | subnet-id)
 *     end
 */

@ResourceName(parent = "nlb", value = "subnet-mapping")
public class SubnetMappingResource extends AwsResource {

    private String allocationId;
    private String ipAddress;
    private String subnetId;

    /**
     *  The allocation id associated with the elastic ip (Optional)
     */
    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    /**
     *  The public ip associated with the elastic ip (Optional)
     */
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     *  The subnet associated with the nlb (Optional)
     */
    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String primaryKey() {
        return String.format("%s/%s/%s", getAllocationId(), getIpAddress(), getSubnetId());
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
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
    }

    @Override
    public void delete() {
        if (parentResource().change() instanceof Delete) {
            return;
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getAllocationId() != null) {
            sb.append("subnet mapping - " + getAllocationId());
        } else {
            sb.append("subnet mapping ");
        }

        return sb.toString();
    }

    public SubnetMapping toSubnetMapping() {
        return SubnetMapping.builder()
                .allocationId(getAllocationId())
                .subnetId(getSubnetId())
                .build();
    }
}
