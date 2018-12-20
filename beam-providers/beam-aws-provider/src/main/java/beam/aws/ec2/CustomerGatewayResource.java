package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CustomerGateway;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.GatewayType;

import java.text.MessageFormat;
import java.util.Set;

/**
 * Create Customer Gateway based on teh provided Public IP.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::customer-gateway customer-gateway-example
 *         public-ip: 38.140.23.146
 *
 *         tags:
 *             Name: peering-connection-example
 *         end
 *     end
 */
@ResourceName("customer-gateway")
public class CustomerGatewayResource extends Ec2TaggableResource<CustomerGateway> {

    private String customerGatewayId;
    private String publicIp;
    private int bgpAsn;

    public String getCustomerGatewayId() {
        return customerGatewayId;
    }

    public void setCustomerGatewayId(String customerGatewayId) {
        this.customerGatewayId = customerGatewayId;
    }

    /**
     * Public IP address for the gateway's external interface. See `Customer Gateway <https://docs.aws.amazon.com/vpc/latest/adminguide/Introduction.html/>`_. (Required)
     */
    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    /**
     * the Border Gateway Protocol Autonomous System Number of the gateway.
     */
    public int getBgpAsn() {
        return bgpAsn;
    }

    public void setBgpAsn(int bgpAsn) {
        this.bgpAsn = bgpAsn;
    }

    @Override
    protected String getId() {
        return getCustomerGatewayId();
    }

    @Override
    protected void doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        DescribeCustomerGatewaysResponse response = client.describeCustomerGateways(r -> r.customerGatewayIds(getCustomerGatewayId()));

        if (!response.customerGateways().isEmpty()) {
            CustomerGateway customerGateway = response.customerGateways().get(0);
            setPublicIp(customerGateway.ipAddress());
        } else {
            throw new BeamException(MessageFormat.format("Customer Gateway - {0} not found.", getCustomerGatewayId()));
        }
    }

    @Override
    protected void doCreate() {
        CreateCustomerGatewayRequest.Builder builder = CreateCustomerGatewayRequest.builder();
        builder.publicIp(getPublicIp());
        builder.type(GatewayType.IPSEC_1);

        if (getBgpAsn() > 0) {
            builder.bgpAsn(getBgpAsn());
        } else {
            builder.bgpAsn(65000);
        }

        Ec2Client client = createClient(Ec2Client.class);

        CreateCustomerGatewayResponse response = client.createCustomerGateway(builder.build());

        setCustomerGatewayId(response.customerGateway().customerGatewayId());
    }

    @Override
    protected void doUpdate(AwsResource config, Set<String> changedProperties) {

    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.deleteCustomerGateway(r -> r.customerGatewayId(getCustomerGatewayId()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getCustomerGatewayId() != null) {
            sb.append(getCustomerGatewayId());

        } else {
            sb.append("Customer Gateway");
        }

        return sb.toString();
    }
}
