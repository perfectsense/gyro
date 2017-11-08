package beam.aws.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.psddev.dari.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NatGatewayResource extends AWSResource<NatGateway> {
    private String id;
    private String elasticIp;
    private String privateIp;
    private BeamReference subnet;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElasticIp() {
        return elasticIp;
    }

    public void setElasticIp(String elasticIp) {
        this.elasticIp = elasticIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public BeamReference getSubnet() {
        return subnet;
    }

    public void setSubnet(BeamReference subnet) {
        this.subnet = subnet;
    }

    @Override
    public String awsId() {
        return getId();
    }

    @Override
    public List<String> diffIds() {
        SubnetResource subnetResource = (SubnetResource) getSubnet().resolve();
        return Arrays.asList(subnetResource.getAvailabilityZone(), subnetResource.getCidrBlock(), getElasticIp());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, NatGateway natGateway) {
        setId(natGateway.getNatGatewayId());
        if (!ObjectUtils.isBlank(natGateway.getNatGatewayAddresses())) {
            NatGatewayAddress natGatewayAddress = natGateway.getNatGatewayAddresses().get(0);
            setElasticIp(natGatewayAddress.getPublicIp());
            setPrivateIp(natGatewayAddress.getPrivateIp());
        }
    }

    private String findAllocationId(AmazonEC2Client client, String elasticIp) {
        DescribeAddressesRequest daRequest = new DescribeAddressesRequest();
        daRequest.setPublicIps(Arrays.asList(elasticIp));

        DescribeAddressesResult daResult = client.describeAddresses(daRequest);

        for (Address address : daResult.getAddresses()) {
            if (address.getAssociationId() != null) {
                DisassociateAddressRequest diaRequest = new DisassociateAddressRequest();
                diaRequest.setAssociationId(address.getAssociationId());
                client.disassociateAddress(diaRequest);
            }

            return address.getAllocationId();
        }

        return null;
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        AllocateAddressRequest alaRequest = new AllocateAddressRequest();

        alaRequest.setDomain(DomainType.Vpc);
        String allocationId;

        if (getElasticIp() == null) {
            AllocateAddressResult alaResult = client.allocateAddress(alaRequest);
            setElasticIp(alaResult.getPublicIp());
            allocationId = alaResult.getAllocationId();
        } else {
            allocationId = findAllocationId(client, getElasticIp());
        }

        SubnetResource subnetResource = (SubnetResource) getSubnet().resolve();
        CreateNatGatewayRequest cngRequest = new CreateNatGatewayRequest();
        cngRequest.setAllocationId(allocationId);
        cngRequest.setSubnetId(subnetResource.getSubnetId());

        CreateNatGatewayResult cngResult = (CreateNatGatewayResult) executeService(() -> client.createNatGateway(cngRequest));
        setId(cngResult.getNatGateway().getNatGatewayId());

        boolean available = false;
        while (!available) {
            DescribeNatGatewaysRequest dngRequest = new DescribeNatGatewaysRequest();
            dngRequest.setFilter(Arrays.asList(
                    new Filter("nat-gateway-id").withValues(getId())));

            DescribeNatGatewaysResult dngResult = client.describeNatGateways(dngRequest);
            for (NatGateway natGateway : dngResult.getNatGateways()) {
                if ("available".equals(natGateway.getState())) {
                    available = true;
                    setId(natGateway.getNatGatewayId());
                } else if ("failed".equals(natGateway.getState())) {
                    throw new BeamException("Fail to create " + toDisplayString());
                }
            }
        }
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, NatGateway> current, Set<String> changedProperties) {

    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonEC2Client client = createClient(AmazonEC2Client.class, cloud.getProvider());
        DeleteNatGatewayRequest dngRequest = new DeleteNatGatewayRequest();
        dngRequest.setNatGatewayId(getId());

        client.deleteNatGateway(dngRequest);
    }

    @Override
    public String toDisplayString() {
        SubnetResource subnetResource = (SubnetResource) getSubnet().resolve();
        return String.format("NAT gateway %s with %s",
                subnetResource.getName(), getElasticIp() != null ? getElasticIp() : "a new elastic ip");
    }
}
