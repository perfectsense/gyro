package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ModifySubnetAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.util.Set;

/**
 * Create a subnet in a VPC.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::subnet example-subnet
 *         vpc-id: $(aws::vpc example-vpc | vpc-id)
 *         availability-zone: us-east-1a
 *         cidr-block: 10.0.0.0/24
 *     end
 */
@ResourceName("subnet")
public class SubnetResource extends Ec2TaggableResource<Subnet> {

    private String vpcId;
    private String cidrBlock;
    private String availabilityZone;
    private Boolean mapPublicIpOnLaunch;
    private String subnetId;

    /**
     * The ID of the VPC to create the subnet in. (Required)
     */
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    /**
     * The IPv4 network range for the subnet, in CIDR notation. (Required)
     */
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    /**
     * The name of the availablity zone to create this subnet (ex. ``us-east-1a``).
     */
    @ResourceDiffProperty
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     * Assign a public IPv4 address to network interfaces created in this subnet.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(Boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getId() {
        return getSubnetId();
    }

    @Override
    public void doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getSubnetId())) {
            throw new BeamException("subnet-id is missing, unable to load subnet.");
        }

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(getSubnetId())
                .build();

        for (Subnet subnet : client.describeSubnets(request).subnets()) {
            String subnetId = subnet.subnetId();

            setAvailabilityZone(subnet.availabilityZone());
            setCidrBlock(subnet.cidrBlock());
            setMapPublicIpOnLaunch(subnet.mapPublicIpOnLaunch());
            setSubnetId(subnetId);
            break;
        }
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(getAvailabilityZone())
                .cidrBlock(getCidrBlock())
                .vpcId(getVpcId())
                .build();

        CreateSubnetResponse response = client.createSubnet(request);

        setSubnetId(response.subnet().subnetId());

        modifyAttribute(client);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        modifyAttribute(client);
    }

    private void modifyAttribute(Ec2Client client) {
        if (getMapPublicIpOnLaunch() != null) {
            ModifySubnetAttributeRequest request = ModifySubnetAttributeRequest.builder()
                    .subnetId(getSubnetId())
                    .mapPublicIpOnLaunch(AttributeBooleanValue.builder().value(getMapPublicIpOnLaunch()).build())
                    .build();

            client.modifySubnetAttribute(request);
        }
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        // Network interfaces may still be detaching, so check and wait
        // before deleting the subnet.
        while (true) {
            DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder()
                    .filters(Filter.builder()
                            .name("subnet-id")
                            .values(getSubnetId()).build())
                    .build();

            if (client.describeNetworkInterfaces(request).networkInterfaces().isEmpty()) {
                break;
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                break;
            }
        }

        DeleteSubnetRequest request = DeleteSubnetRequest.builder()
                .subnetId(getSubnetId())
                .build();

        client.deleteSubnet(request);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String subnetId = getSubnetId();

        if (subnetId != null) {
            sb.append(subnetId);

        } else {
            sb.append("subnet");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(getCidrBlock());
        }

        String availabilityZone = getAvailabilityZone();

        if (availabilityZone != null) {
            sb.append(" in ");
            sb.append(availabilityZone);
        }

        return sb.toString();
    }
}
