package beam.aws;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.BeamContextKey;
import beam.lang.BeamLiteral;
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

@ResourceName("subnet")
public class SubnetResource extends TaggableResource<Subnet> {

    private String subnetId;
    private String availabilityZone;
    private String cidrBlock;
    private Boolean mapPublicIpOnLaunch;
    private String vpcId;

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @ResourceDiffProperty
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(Boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getId() {
        return getSubnetId();
    }

    @Override
    public void refresh(BeamCredentials credentials) {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getSubnetId())) {
            throw new BeamException("subnet-id is missing, unable to load subnet.");
        }

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(getSubnetId())
                .build();

        for (Subnet subnet : client.describeSubnets(request).subnets()) {
            doInit(subnet);
            break;
        }
    }

    @Override
    protected void doInit(Subnet subnet) {
        String subnetId = subnet.subnetId();

        setAvailabilityZone(subnet.availabilityZone());
        setCidrBlock(subnet.cidrBlock());
        setMapPublicIpOnLaunch(subnet.mapPublicIpOnLaunch());
        setSubnetId(subnetId);
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
