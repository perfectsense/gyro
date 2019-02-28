package gyro.aws.ec2;

import gyro.aws.AwsResourceQuery;
import gyro.core.diff.ResourceName;
import gyro.lang.ast.query.ApiFilterable;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Filter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ResourceName("vpc")
public class VpcResourceQuery extends AwsResourceQuery<VpcResource> {

    private String cidrBlock;
    private String ipv4CidrBlock;
    private String cidrBlockAssociationId;
    private String cidrBlockAssociationState;
    private String dhcpOptionsId;
    private String ipv6CidrBlock;
    private String ipv6CidrBlockAssociationId;
    private String ipv6CidrBlockAssociationState;
    private String isDefault;
    private String ownerId;
    private String state;
    private String tagKey;
    private String vpcId;

    @ApiFilterable(filter = "cidr")
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    @ApiFilterable(filter = "cidr-block-association.cidr-block")
    public String getIpv4CidrBlock() {
        return ipv4CidrBlock;
    }

    public void setIpv4CidrBlock(String ipv4CidrBlock) {
        this.ipv4CidrBlock = ipv4CidrBlock;
    }

    @ApiFilterable(filter = "cidr-block-association.association-id")
    public String getCidrBlockAssociationId() {
        return cidrBlockAssociationId;
    }

    public void setCidrBlockAssociationId(String cidrBlockAssociationId) {
        this.cidrBlockAssociationId = cidrBlockAssociationId;
    }

    @ApiFilterable(filter = "cidr-block-association.state")
    public String getCidrBlockAssociationState() {
        return cidrBlockAssociationState;
    }

    public void setCidrBlockAssociationState(String cidrBlockAssociationState) {
        this.cidrBlockAssociationState = cidrBlockAssociationState;
    }

    @ApiFilterable(filter = "dhcp-options-id")
    public String getDhcpOptionsId() {
        return dhcpOptionsId;
    }

    public void setDhcpOptionsId(String dhcpOptionsId) {
        this.dhcpOptionsId = dhcpOptionsId;
    }

    @ApiFilterable(filter = "ipv6-cidr-block-association.ipv6-cidr-block")
    public String getIpv6CidrBlock() {
        return ipv6CidrBlock;
    }

    public void setIpv6CidrBlock(String ipv6CidrBlock) {
        this.ipv6CidrBlock = ipv6CidrBlock;
    }

    @ApiFilterable(filter = "ipv6-cidr-block-association.association-id")
    public String getIpv6CidrBlockAssociationId() {
        return ipv6CidrBlockAssociationId;
    }

    public void setIpv6CidrBlockAssociationId(String ipv6CidrBlockAssociationId) {
        this.ipv6CidrBlockAssociationId = ipv6CidrBlockAssociationId;
    }

    @ApiFilterable(filter = "ipv6-cidr-block-association.state")
    public String getIpv6CidrBlockAssociationState() {
        return ipv6CidrBlockAssociationState;
    }

    public void setIpv6CidrBlockAssociationState(String ipv6CidrBlockAssociationState) {
        this.ipv6CidrBlockAssociationState = ipv6CidrBlockAssociationState;
    }

    @ApiFilterable(filter = "isDefault")
    public String getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }

    @ApiFilterable(filter = "owner-id")
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @ApiFilterable(filter = "state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ApiFilterable(filter = "tag-key")
    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    @ApiFilterable(filter = "vpc-id")
    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public List query(Map<String, String> filters) {
        Ec2Client client = createClient(Ec2Client.class);
        List<Filter> queryFilters = queryFilters(filters);

        return client.describeVpcs(r -> r.filters(queryFilters)).vpcs()
            .stream()
            .map(v -> new VpcResource(client, v))
            .collect(Collectors.toList());
    }

    @Override
    public List queryAll() {
        Ec2Client client = createClient(Ec2Client.class);

        return client.describeVpcs().vpcs()
            .stream()
            .map(v -> new VpcResource(client, v))
            .collect(Collectors.toList());
    }
}
