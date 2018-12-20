package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamResource;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateEgressOnlyInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DescribeEgressOnlyInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.EgressOnlyInternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;

import java.util.Set;

/**
 * Create an egress only gateway.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::egress-gateway example-egress-gateway
 *         vpc-id: $(aws::vpc vpc-example | vpc-id)
 *     end
 */
@ResourceName("egress-gateway")
public class EgressOnlyInternetGatewayResource extends AwsResource {

    private String vpcId;
    private String gatewayId;

    /**
     * The ID of the VPC to create the egress only internet gateway in. (Required)
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Override
    public boolean refresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeEgressOnlyInternetGatewaysResponse response = client.describeEgressOnlyInternetGateways(
            r -> r.egressOnlyInternetGatewayIds(getGatewayId())
        );

        for (EgressOnlyInternetGateway gateway : response.egressOnlyInternetGateways()) {
            for (InternetGatewayAttachment attachment : gateway.attachments()) {
                setVpcId(attachment.vpcId());

                return true;
            }
        }

        return false;
    }

    @Override
    public void create() {
        Ec2Client client = createClient(Ec2Client.class);

        if (getVpcId() != null) {
            CreateEgressOnlyInternetGatewayResponse response = client.createEgressOnlyInternetGateway(r -> r.vpcId(getVpcId()));
            EgressOnlyInternetGateway gateway = response.egressOnlyInternetGateway();
            setGatewayId(gateway.egressOnlyInternetGatewayId());
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);
        client.deleteEgressOnlyInternetGateway(r -> r.egressOnlyInternetGatewayId(getGatewayId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getGatewayId() != null) {
            sb.append(getGatewayId());
        } else {
            sb.append("egress gateway");
        }

        return sb.toString();
    }
}
