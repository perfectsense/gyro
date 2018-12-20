package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.NatGateway;

import java.util.Set;

/**
 * Creates a Nat Gateway with the specified elastic ip allocation id and subnet id.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::nat-gateway nat-gateway-example
 *         elastic-allocation-id: $(aws::elastic-ip elastic-ip-example-for-nat-gateway | allocation-id)
 *         subnet-id: $(aws::subnet subnet-example-for-nat-gateway | subnet-id )
 *
 *         tags:
 *             Name: elastic-ip-example-for-nat-gateway
 *         end
 *     end
 */
@ResourceName("nat-gateway")
public class NatGatewayResource extends Ec2TaggableResource<NatGateway> {

    private String natGatewayId;
    private String elasticAllocationId;
    private String subnetId;

    /**
     * Nat Gateway id when created.
     */
    public String getNatGatewayId() {
        return natGatewayId;
    }

    public void setNatGatewayId(String natGatewayId) {
        this.natGatewayId = natGatewayId;
    }

    /**
     * Allocation id of the elastic ip for the nat gateway. (Required)
     */
    public String getElasticAllocationId() {
        return elasticAllocationId;
    }

    public void setElasticAllocationId(String elasticAllocationId) {
        this.elasticAllocationId = elasticAllocationId;
    }

    /**
     * Subnet id for the nat gateway. (Required)
     */
    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    protected String getId() {
        return getNatGatewayId();
    }

    @Override
    public boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeNatGatewaysResponse response = client.describeNatGateways(
            r -> r.natGatewayIds(getNatGatewayId())
                .maxResults(5)
        );

        if (!response.natGateways().isEmpty()) {
            NatGateway natGateway = response.natGateways().get(0);
            setSubnetId(natGateway.subnetId());
            setElasticAllocationId(natGateway.natGatewayAddresses().get(0).allocationId());

            return true;
        }

        return false;
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateNatGatewayResponse response = client.createNatGateway(
            r -> r.allocationId(getElasticAllocationId())
                .subnetId(getSubnetId())
        );

        NatGateway natGateway = response.natGateway();
        setNatGatewayId(natGateway.natGatewayId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteNatGateway(
            r -> r.natGatewayId(getNatGatewayId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getNatGatewayId() != null) {
            sb.append(getNatGatewayId());

        } else {
            sb.append("Nat Gateway");
        }

        return sb.toString();
    }
}
