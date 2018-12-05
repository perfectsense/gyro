package beam.aws.resources;

import beam.core.BeamCredentials;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.BeamContextKey;
import beam.lang.BeamLiteral;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcAttributeName;

import java.util.Set;

@ResourceName("vpc")
public class VpcResource extends TaggableResource<Vpc> {

    private String vpcId;
    private String cidrBlock;
    private Boolean enableDnsHostnames;
    private Boolean enableDnsSupport;

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
        return enableDnsHostnames;
    }

    public void setEnableDnsHostnames(Boolean enableDnsHostnames) {
        this.enableDnsHostnames = enableDnsHostnames;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableDnsSupport() {
        return enableDnsSupport;
    }

    public void setEnableDnsSupport(Boolean enableDnsSupport) {
        this.enableDnsSupport = enableDnsSupport;
    }

    @Override
    public void refresh(BeamCredentials credentials) {
        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getVpcId())) {
            throw new BeamException("vpc-id is missing, unable to load vpc.");
        }

        DescribeVpcsRequest request = DescribeVpcsRequest.builder()
                .vpcIds(getVpcId())
                .build();

        for (Vpc vpc : client.describeVpcs(request).vpcs()) {
            doInit(vpc);
            break;
        }
    }

    @Override
    protected void doInit(Vpc vpc) {
        Ec2Client client = createClient(Ec2Client.class);
        String vpcId = vpc.vpcId();

        setCidrBlock(vpc.cidrBlock());
        setVpcId(vpcId);

        // VPC attributes.
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
    }

    @Override
    protected void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateVpcRequest request = CreateVpcRequest.builder()
                .cidrBlock(getCidrBlock())
                .build();

        CreateVpcResponse response = client.createVpc(request);

        setVpcId(response.vpc().vpcId());
        addReferable(new BeamContextKey(null, "vpc-id"), new BeamLiteral(getVpcId()));

        modifyAttributes(client);
    }

    @Override
    protected void doUpdate(AwsResource current, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        modifyAttributes(client);
    }

    private void modifyAttributes(Ec2Client client) {
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
}
