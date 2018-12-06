package beam.aws;

import beam.core.BeamCredentials;
import beam.core.diff.ResourceName;
import beam.lang.BeamContextKey;
import beam.lang.BeamLiteral;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;

import java.util.Set;

@ResourceName("internet-gateway")
public class InternetGatewayResource extends TaggableResource<InternetGateway> {

    private String internetGatewayId;
    private String vpcId;

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

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
    public void refresh(BeamCredentials cloud) {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeInternetGatewaysResponse response = client.describeInternetGateways(
                r -> r.internetGatewayIds(getInternetGatewayId())
        );

        for (InternetGateway gateway : response.internetGateways()) {
            for (InternetGatewayAttachment attachment : gateway.attachments()) {
                setVpcId(attachment.vpcId());

                break;
            }
        }
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

        for (InternetGateway gateway : client.describeInternetGateways(
                r -> r.internetGatewayIds(getInternetGatewayId())).internetGateways()) {
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
