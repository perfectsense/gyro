package beam.aws.resources;

import beam.aws.AwsCredentials;
import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.psddev.dari.util.ObjectUtils;

import java.util.Arrays;
import java.util.Set;

public class SubnetResource extends TaggableResource<Subnet> {

    private String subnetId;
    private String availabilityZone;
    private String cidrBlock;
    private Boolean mapPublicIpOnLaunch;
    private VpcResource vpc;

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

    public VpcResource getVpc() {
        return vpc;
    }

    public void setVpc(VpcResource vpc) {
        this.vpc = vpc;
    }

    public String getId() {
        return getSubnetId();
    }

    @Override
    public void refresh(BeamCredentials credentials) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class);

        if (ObjectUtils.isBlank(getSubnetId())) {
            throw new BeamException("subnet-id is missing, unable to load subnet.");
        }

        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.withSubnetIds(getSubnetId());

        for (Subnet subnet : client.describeSubnets(request).getSubnets()) {
            doInit(subnet);
            break;
        }
    }

    @Override
    protected void doInit(Subnet subnet) {
        String subnetId = subnet.getSubnetId();

        setAvailabilityZone(subnet.getAvailabilityZone());
        setCidrBlock(subnet.getCidrBlock());
        setMapPublicIpOnLaunch(subnet.getMapPublicIpOnLaunch());
        setSubnetId(subnetId);
    }

    @Override
    protected void doCreate() {
        AmazonEC2Client client = createClient(AmazonEC2Client.class);
        CreateSubnetRequest csRequest = new CreateSubnetRequest();

        csRequest.setAvailabilityZone(getAvailabilityZone());
        csRequest.setCidrBlock(getCidrBlock());
        csRequest.setVpcId(getVpc().getVpcId());
        setSubnetId(client.createSubnet(csRequest).getSubnet().getSubnetId());
        modifyAttribute(client);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class);

        modifyAttribute(client);
    }

    private void modifyAttribute(AmazonEC2Client client) {
        Boolean mapPublicIpOnLaunch = getMapPublicIpOnLaunch();

        if (mapPublicIpOnLaunch != null) {
            ModifySubnetAttributeRequest msaRequest = new ModifySubnetAttributeRequest();

            msaRequest.setSubnetId(getSubnetId());
            msaRequest.setMapPublicIpOnLaunch(mapPublicIpOnLaunch);
            client.modifySubnetAttribute(msaRequest);
        }
    }

    @Override
    public void delete() {
        AmazonEC2Client client = createClient(AmazonEC2Client.class);

        // Network interfaces may still be detaching, so check and wait
        // before deleting the subnet.
        while (true) {
            DescribeNetworkInterfacesRequest dniRequest = new DescribeNetworkInterfacesRequest();

            dniRequest.setFilters(Arrays.asList(
                    new Filter("subnet-id").withValues(getSubnetId())));

            if (client.
                    describeNetworkInterfaces(dniRequest).
                    getNetworkInterfaces().
                    isEmpty()) {
                break;
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                break;
            }
        }

        DeleteSubnetRequest dsRequest = new DeleteSubnetRequest();

        dsRequest.setSubnetId(getSubnetId());
        client.deleteSubnet(dsRequest);
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
