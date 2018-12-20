package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVpcPeeringConnectionResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsResponse;
import software.amazon.awssdk.services.ec2.model.VpcPeeringConnection;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.text.MessageFormat;
import java.util.Set;

/**
 * Create a Peering Connection between two VPC.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::peering-connection peering-connection-example
 *         vpc-id: $(aws::vpc vpc-example-for-peering-connection-1 | vpc-id)
 *         peer-vpc-id: $(aws::vpc vpc-example-for-peering-connection-2 | vpc-id)
 *         owner-id: '572681481110'
 *         region: 'us-east-1'
 *
 *         tags:
 *             Name: peering-connection-example
 *         end
 *     end
 */
@ResourceName("peering-connection")
public class PeeringConectionResource extends Ec2TaggableResource<VpcPeeringConnection> {

    private String vpcId;
    private String peerVpcId;
    private String ownerId;
    private String region;
    private String peeringConnectionId;

    /**
     * Requester VPC ID. See `Creating and Accepting Peering Connection <https://docs.aws.amazon.com/vpc/latest/peering/create-vpc-peering-connection.html/>`_. (Required)
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    /**
     * Accepter VPC ID. (Required)
     */
    public String getPeerVpcId() {
        return peerVpcId;
    }

    public void setPeerVpcId(String peerVpcId) {
        this.peerVpcId = peerVpcId;
    }

    /**
     * Accepter Account ID. (Required)
     */
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Accepter Region. (Required)
     */
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPeeringConnectionId() {
        return peeringConnectionId;
    }

    public void setPeeringConnectionId(String peeringConnectionId) {
        this.peeringConnectionId = peeringConnectionId;
    }

    @Override
    protected String getId() {
        return getPeeringConnectionId();
    }

    @Override
    protected void doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeVpcPeeringConnectionsResponse response = client.describeVpcPeeringConnections(r -> r.vpcPeeringConnectionIds(getId()));

        if (!response.vpcPeeringConnections().isEmpty()) {
            VpcPeeringConnection vpcPeeringConnection = response.vpcPeeringConnections().get(0);
            setPeeringConnectionId(vpcPeeringConnection.vpcPeeringConnectionId());
            setOwnerId(vpcPeeringConnection.accepterVpcInfo().ownerId());
            setVpcId(vpcPeeringConnection.accepterVpcInfo().vpcId());
        } else {
            throw new BeamException(MessageFormat.format("VPC Peering Connection - {0} not found.", getPeeringConnectionId()));
        }
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateVpcPeeringConnectionResponse response = client.createVpcPeeringConnection(
            r -> r.vpcId(getVpcId())
                .peerVpcId(getPeerVpcId())
                .peerOwnerId(getOwnerId())
                .peerRegion(getRegion())
        );

        VpcPeeringConnection vpcPeeringConnection = response.vpcPeeringConnection();
        setPeeringConnectionId(vpcPeeringConnection.vpcPeeringConnectionId());

        client.acceptVpcPeeringConnection(
            r -> r.vpcPeeringConnectionId(getPeeringConnectionId())
        );
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteVpcPeeringConnection(r -> r.vpcPeeringConnectionId(getPeeringConnectionId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getPeeringConnectionId() != null) {
            sb.append(getPeeringConnectionId());

        } else {
            sb.append("Peering Connection");
        }

        return sb.toString();
    }
}
