package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;

public class InternetGatewayResource extends TaggableEC2Resource<InternetGateway> {

    private String internetGatewayId;
    private BeamReference vpc;

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    public BeamReference getVpc() {
        return newParentReference(VpcResource.class, vpc);
    }

    public void setVpc(BeamReference vpc) {
        this.vpc = vpc;
    }

    @Override
    public String awsId() {
        return getInternetGatewayId();
    }

    @Override
    public List<String> diffIds() {
        BeamReference vpc = getVpc();

        if (vpc != null) {
            return Arrays.asList(vpc.awsId());

        } else {
            return Arrays.asList(getInternetGatewayId());
        }
    }

    @Override
    protected void doInit(AWSCloud cloud, BeamResourceFilter filter, InternetGateway ig) {
        setInternetGatewayId(ig.getInternetGatewayId());
    }

    @Override
    protected void doCreate(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        CreateInternetGatewayRequest cigRequest = new CreateInternetGatewayRequest();

        setInternetGatewayId(client.createInternetGateway(cigRequest).getInternetGateway().getInternetGatewayId());

        // Attach the internet gateway to a VPC?
        BeamReference vpc = getVpc();

        if (vpc != null) {
            AttachInternetGatewayRequest aigRequest = new AttachInternetGatewayRequest();

            aigRequest.setInternetGatewayId(getInternetGatewayId());
            aigRequest.setVpcId(vpc.awsId());
            client.attachInternetGateway(aigRequest);
        }
    }

    @Override
    protected void doUpdate(AWSCloud cloud, AWSResource<InternetGateway> current, Set<String> changedProperties) {
        InternetGatewayResource currentIg = (InternetGatewayResource) current;
        BeamReference currentVpc = currentIg.getVpc();
        BeamReference pendingVpc = getVpc();

        // Detach the internet gateway to a VPC?
        if (currentVpc != null && pendingVpc == null) {
            AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
            DetachInternetGatewayRequest digRequest = new DetachInternetGatewayRequest();

            digRequest.setInternetGatewayId(getInternetGatewayId());
            digRequest.setVpcId(currentVpc.awsId());
            client.detachInternetGateway(digRequest);
        }
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());

        // Detach from all VPCs.
        DescribeInternetGatewaysRequest dsigRequest = new DescribeInternetGatewaysRequest();

        dsigRequest.setInternetGatewayIds(Arrays.asList(getInternetGatewayId()));

        for (InternetGateway ig : client.
                describeInternetGateways(dsigRequest).
                getInternetGateways()) {

            for (InternetGatewayAttachment iga : ig.getAttachments()) {
                DetachInternetGatewayRequest deigRequest = new DetachInternetGatewayRequest();

                deigRequest.setVpcId(iga.getVpcId());
                deigRequest.setInternetGatewayId(getInternetGatewayId());
                client.detachInternetGateway(deigRequest);
            }
        }

        // Delete the internet gateway.
        DeleteInternetGatewayRequest digRequest = new DeleteInternetGatewayRequest();

        digRequest.setInternetGatewayId(getInternetGatewayId());
        client.deleteInternetGateway(digRequest);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String igId = getInternetGatewayId();

        if (igId != null) {
            sb.append(igId);

        } else {
            sb.append("internet gateway");
        }

        return sb.toString();
    }
}
