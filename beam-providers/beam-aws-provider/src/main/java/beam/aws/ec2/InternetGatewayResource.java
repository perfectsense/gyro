package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.core.diff.ResourceOutput;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;

import java.util.Set;
import java.util.UUID;

/**
 * Create an internet gateway.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::internet-gateway example-gateway
 *         vpc-id: $(aws::vpc vpc-example | vpc-id)
 *     end
 */
@ResourceName("internet-gateway")
public class InternetGatewayResource extends Ec2TaggableResource<InternetGateway> {

    private String internetGatewayId;
    private String vpcId;

    @ResourceOutput
    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    /**
     * The ID of the VPC to create an internet gateway in.
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    protected String getId() {
        return getInternetGatewayId();
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        try {
            DescribeInternetGatewaysResponse response = client.describeInternetGateways(
                r -> r.internetGatewayIds(getInternetGatewayId())
            );

            for (InternetGateway gateway : response.internetGateways()) {
                for (InternetGatewayAttachment attachment : gateway.attachments()) {
                    setVpcId(attachment.vpcId());

                    break;
                }
            }
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateInternetGatewayResponse response = client.createInternetGateway();

        setInternetGatewayId(response.internetGateway().internetGatewayId());

        if (getVpcId() != null) {
            client.attachInternetGateway(r -> r.internetGatewayId(getInternetGatewayId())
                    .vpcId(getVpcId())
            );
        }
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        for (InternetGateway gateway : client.describeInternetGateways(r -> r.internetGatewayIds(getInternetGatewayId())).internetGateways()) {
            for (InternetGatewayAttachment attachment : gateway.attachments()) {
                client.detachInternetGateway(
                    r -> r.internetGatewayId(getInternetGatewayId()).vpcId(attachment.vpcId())
                );
            }
        }

        client.deleteInternetGateway(r -> r.internetGatewayId(getInternetGatewayId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getInternetGatewayId() != null) {
            sb.append(getInternetGatewayId());

        } else {
            sb.append("internet gateway");
        }

        return sb.toString();
    }

}
