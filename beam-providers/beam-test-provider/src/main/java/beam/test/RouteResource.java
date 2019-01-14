package beam.test;

import beam.core.diff.ResourceName;

@ResourceName("fake-route")
public class RouteResource extends FakeResource {

    private String routeTableId;
    private String destinationCidrBlock;
    private String destinationIpv6CidrBlock;
    private String destinationPrefixListId;
    private String egressOnlyInternetGatewayId;
    private String gatewayId;
    private String instanceId;
    private String instanceOwnerId;
    private String natGatewayId;
    private String networkInterfaceId;
    private String transitGatewayId;

    public String getRouteTableId() {
        return routeTableId;
    }

    public void setRouteTableId(String routeTableId) {
        this.routeTableId = routeTableId;
    }

    public String getDestinationCidrBlock() {
        return destinationCidrBlock;
    }

    public void setDestinationCidrBlock(String destinationCidrBlock) {
        this.destinationCidrBlock = destinationCidrBlock;
    }

    public String getDestinationIpv6CidrBlock() {
        return destinationIpv6CidrBlock;
    }

    public void setDestinationIpv6CidrBlock(String destinationIpv6CidrBlock) {
        this.destinationIpv6CidrBlock = destinationIpv6CidrBlock;
    }

    public String getDestinationPrefixListId() {
        return destinationPrefixListId;
    }

    public void setDestinationPrefixListId(String destinationPrefixListId) {
        this.destinationPrefixListId = destinationPrefixListId;
    }

    public String getEgressOnlyInternetGatewayId() {
        return egressOnlyInternetGatewayId;
    }

    public void setEgressOnlyInternetGatewayId(String egressOnlyInternetGatewayId) {
        this.egressOnlyInternetGatewayId = egressOnlyInternetGatewayId;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceOwnerId() {
        return instanceOwnerId;
    }

    public void setInstanceOwnerId(String instanceOwnerId) {
        this.instanceOwnerId = instanceOwnerId;
    }

    public String getNatGatewayId() {
        return natGatewayId;
    }

    public void setNatGatewayId(String natGatewayId) {
        this.natGatewayId = natGatewayId;
    }

    public String getNetworkInterfaceId() {
        return networkInterfaceId;
    }

    public void setNetworkInterfaceId(String networkInterfaceId) {
        this.networkInterfaceId = networkInterfaceId;
    }

    public String getTransitGatewayId() {
        return transitGatewayId;
    }

    public void setTransitGatewayId(String transitGatewayId) {
        this.transitGatewayId = transitGatewayId;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("fake route ");
        sb.append(getDestinationCidrBlock());

        if (getGatewayId() != null) {
            sb.append(" through gateway ");
            sb.append(getGatewayId());
        } else if (getInstanceId() != null) {
            sb.append(" through instance ");
            sb.append(getInstanceId());
        } else if (getGatewayId() != null) {
            sb.append(" through nat gateway");
            sb.append(getNatGatewayId());
        } else if (getNetworkInterfaceId() != null) {
            sb.append(" through network interface ");
            sb.append(getNetworkInterfaceId());
        } else if (getTransitGatewayId() != null) {
            sb.append(" through transit gateway ");
            sb.append(getTransitGatewayId());
        }

        return sb.toString();
    }

}
