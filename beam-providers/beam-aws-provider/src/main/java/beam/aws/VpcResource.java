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
import software.amazon.awssdk.services.ec2.model.ClassicLinkDnsSupport;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcClassicLinkDnsSupportResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcClassicLinkResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcAttributeName;
import software.amazon.awssdk.services.ec2.model.VpcClassicLink;

import java.util.Set;

@ResourceName("vpc")
public class VpcResource extends TaggableResource<Vpc> {

    private String vpcId;
    private String cidrBlock;
    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;
    private String dhcpOptionsId;
    private String instanceTenancy;
    private Boolean enableClassicLink;
    private Boolean enableClassicLinkDnsSupport;

    // Read-only
    private String ownerId;
    private Boolean defaultVpc;

    public String getId() {
        return getVpcId();
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @ResourceDiffProperty
    public String getCidrBlock() {
        return cidrBlock;
    }

    public void setCidrBlock(String cidrBlock) {
        this.cidrBlock = cidrBlock;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsHostnames() {
        if (enableDnsHostnames == null) {
            enableDnsHostnames = true;
        }

        return enableDnsHostnames;
    }

    public void setEnableDnsHostnames(Boolean enableDnsHostnames) {
        this.enableDnsHostnames = enableDnsHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsSupport() {
        if (enableDnsSupport == null) {
            enableDnsSupport = true;
        }

        return enableDnsSupport;
    }

    public void setEnableDnsSupport(Boolean enableDnsSupport) {
        this.enableDnsSupport = enableDnsSupport;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDhcpOptionsId() {
        return dhcpOptionsId;
    }

    public void setDhcpOptionsId(String dhcpOptionsId) {
        this.dhcpOptionsId = dhcpOptionsId;
    }

    public String getInstanceTenancy() {
        return instanceTenancy;
    }

    public void setInstanceTenancy(String instanceTenancy) {
        this.instanceTenancy = instanceTenancy;
    }

    public Boolean getDefaultVpc() {
        if (defaultVpc == null) {
            defaultVpc = false;
        }

        return defaultVpc;
    }

    public void setDefaultVpc(Boolean defaultVpc) {
        this.defaultVpc = defaultVpc;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableClassicLink() {
        if (enableClassicLink == null) {
            enableClassicLink = false;
        }

        return enableClassicLink;
    }

    public void setEnableClassicLink(Boolean enableClassicLink) {
        this.enableClassicLink = enableClassicLink;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableClassicLinkDnsSupport() {
        if (enableClassicLinkDnsSupport == null) {
            enableClassicLinkDnsSupport = false;
        }

        return enableClassicLinkDnsSupport;
    }

    public void setEnablelassicLinkDnsSupport(Boolean enableClassicLinkDnsSupport) {
        this.enableClassicLinkDnsSupport = enableClassicLinkDnsSupport;
    }

    @Override
    public void doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getVpcId())) {
            throw new BeamException("vpc-id is missing, unable to load vpc.");
        }

        DescribeVpcsRequest request = DescribeVpcsRequest.builder()
                .vpcIds(getVpcId())
                .build();

        for (Vpc vpc : client.describeVpcs(request).vpcs()) {
            String vpcId = vpc.vpcId();

            setVpcId(vpcId);
            setCidrBlock(vpc.cidrBlock());
            setInstanceTenancy(vpc.instanceTenancyAsString());
            setDhcpOptionsId(vpc.dhcpOptionsId());
            setOwnerId(vpc.ownerId());
            setDefaultVpc(vpc.isDefault());

            loadSettings(client);
            break;
        }
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateVpcRequest request = CreateVpcRequest.builder()
                .cidrBlock(getCidrBlock())
                .instanceTenancy(getInstanceTenancy())
                .build();

        CreateVpcResponse response = client.createVpc(request);

        Vpc vpc = response.vpc();
        setVpcId(response.vpc().vpcId());
        setOwnerId(vpc.ownerId());
        setInstanceTenancy(vpc.instanceTenancyAsString());

        modifySettings(client);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        modifySettings(client);
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(getVpcId())
                .build();

        client.deleteVpc(request);
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        String vpcId = getVpcId();

        if (vpcId != null) {
            sb.append(vpcId);

        } else {
            sb.append("VPC");
        }

        String cidrBlock = getCidrBlock();

        if (cidrBlock != null) {
            sb.append(' ');
            sb.append(cidrBlock);
        }

        return sb.toString();
    }

    private void loadSettings(Ec2Client client) {
        // DNS Settings
        DescribeVpcAttributeRequest request = DescribeVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .attribute(VpcAttributeName.ENABLE_DNS_HOSTNAMES)
                .build();
        setEnableDnsHostnames(client.describeVpcAttribute(request).enableDnsHostnames().value());

        request = DescribeVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .attribute(VpcAttributeName.ENABLE_DNS_SUPPORT)
                .build();
        setEnableDnsSupport(client.describeVpcAttribute(request).enableDnsSupport().value());

        // ClassicLink
        DescribeVpcClassicLinkResponse clResponse =  client.describeVpcClassicLink(r -> r.vpcIds(getVpcId()));
        for (VpcClassicLink classicLink : clResponse.vpcs()) {
            setEnableClassicLink(classicLink.classicLinkEnabled());
            break;
        }

        DescribeVpcClassicLinkDnsSupportResponse cldResponse =  client.describeVpcClassicLinkDnsSupport(r -> r.vpcIds(getVpcId()));
        for (ClassicLinkDnsSupport classicLink : cldResponse.vpcs()) {
            setEnableClassicLink(classicLink.classicLinkDnsSupported());
            break;
        }
    }

    private void modifySettings(Ec2Client client) {
        // DNS Settings
        if (getEnableDnsHostnames() != null) {
            ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsHostnames(AttributeBooleanValue.builder().value(getEnableDnsHostnames()).build())
                    .build();

            client.modifyVpcAttribute(request);
        }

        if (getEnableDnsSupport() != null) {
            ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsSupport(AttributeBooleanValue.builder().value(getEnableDnsSupport()).build())
                    .build();

            client.modifyVpcAttribute(request);

            request = ModifyVpcAttributeRequest.builder()
                    .vpcId(getVpcId())
                    .enableDnsHostnames(AttributeBooleanValue.builder().value(getEnableDnsHostnames()).build())
                    .build();

            client.modifyVpcAttribute(request);
        }

        // DCHP Options
        if (getDhcpOptionsId() != null) {
            client.associateDhcpOptions(r -> r.dhcpOptionsId(getDhcpOptionsId()).vpcId(getVpcId()));
        }

        // ClassicLink
        if (getEnableClassicLink()) {
            client.enableVpcClassicLink(r -> r.vpcId(getVpcId()));
        } else {
            client.disableVpcClassicLink(r -> r.vpcId(getVpcId()));
        }

        if (getEnableClassicLinkDnsSupport()) {
            client.enableVpcClassicLinkDnsSupport(r -> r.vpcId(getVpcId()));
        } else {
            client.disableVpcClassicLinkDnsSupport(r -> r.vpcId(getVpcId()));
        }

        // Tenancy
        client.modifyVpcTenancy(r -> r.instanceTenancy(getInstanceTenancy()).vpcId(getVpcId()));
    }

}
