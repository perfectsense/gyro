package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.route53.model.Route53Exception;

import java.util.Set;

@ResourceName(parent = "hosted-zone", value = "vpc")
public class Route53VpcResource extends AwsResource {
    private String vpcId;
    private String vpcRegion;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcRegion() {
        return vpcRegion;
    }

    public void setVpcRegion(String vpcRegion) {
        this.vpcRegion = vpcRegion;
    }

    public Route53VpcResource() {

    }

    public Route53VpcResource(String vpcId, String vpcRegion) {
        this.vpcId = vpcId;
        this.vpcRegion = vpcRegion;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        HostedZoneResource parent = getParent();

        try {
            parent.saveVpc(null, getVpcId(), getVpcRegion(), true);
        } catch (Route53Exception ex) {
            if (!ex.awsErrorDetails().errorCode().equalsIgnoreCase("ConflictingDomainExists")) {
                throw ex;
            }
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        HostedZoneResource parent = getParent();

        try {
            parent.saveVpc(null, getVpcId(), getVpcRegion(), false);
        } catch (Route53Exception ex) {
            if (!ex.awsErrorDetails().errorCode().equalsIgnoreCase("LastVpcAssociation")) {
                throw ex;
            }
        }
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("route53 vpc ").append(" [ ");

        if (!ObjectUtils.isBlank(getVpcId())) {
            sb.append(getVpcId()).append(" - ");
        }

        if (!ObjectUtils.isBlank(getVpcRegion())) {
            sb.append(getVpcRegion()).append(" ]");
        }

        return sb.toString();
    }

    @Override
    public String primaryKey() {
        return String.format("%s %s", getVpcId(), getVpcRegion());
    }

    @Override
    public String resourceIdentifier() {
        return null;
    }

    private HostedZoneResource getParent() {
        HostedZoneResource parent = (HostedZoneResource) parentResource();
        if (parent == null) {
            throw new BeamException("Parent hosted zone resource not found.");
        }
        return parent;
    }
}
