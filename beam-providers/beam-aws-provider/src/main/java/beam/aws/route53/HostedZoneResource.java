package beam.aws.route53;

import beam.aws.AwsResource;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.LinkedService;
import software.amazon.awssdk.services.route53.model.ListVpcAssociationAuthorizationsResponse;
import software.amazon.awssdk.services.route53.model.VPC;

import java.util.Set;

@ResourceName("hosted-zone")
public class HostedZoneResource extends AwsResource {

    private String callerReference;
    private String delegationSetId;
    private String comment;
    private Boolean privateZone;
    private String hostedZoneId;
    private String hostedZoneName;
    private Long resourceRecordSetCount;
    private String description;
    private String servicePrincipal;
    private String vpcId;
    private String vpcRegion;

    public String getCallerReference() {
        return callerReference;
    }

    public void setCallerReference(String callerReference) {
        this.callerReference = callerReference;
    }

    public String getDelegationSetId() {
        return delegationSetId;
    }

    public void setDelegationSetId(String delegationSetId) {
        this.delegationSetId = delegationSetId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getPrivateZone() {
        return privateZone;
    }

    public void setPrivateZone(Boolean privateZone) {
        this.privateZone = privateZone;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public String getHostedZoneName() {
        return hostedZoneName;
    }

    public void setHostedZoneName(String hostedZoneName) {
        this.hostedZoneName = hostedZoneName;
    }

    public Long getResourceRecordSetCount() {
        return resourceRecordSetCount;
    }

    public void setResourceRecordSetCount(Long resourceRecordSetCount) {
        this.resourceRecordSetCount = resourceRecordSetCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServicePrincipal() {
        return servicePrincipal;
    }

    public void setServicePrincipal(String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcRegion() {
        return vpcRegion;
    }

    public void setVpcRegion(String vpcRegion) {
        this.vpcRegion = vpcRegion;
    }

    @Override
    public boolean refresh() {
        Route53Client client = createClient(Route53Client.class);

        GetHostedZoneResponse response = client.getHostedZone(
            r -> r.id(getHostedZoneId())
        );

        DelegationSet delegationSet = response.delegationSet();
        setDelegationSetId(delegationSet.id());

        HostedZone hostedZone = response.hostedZone();
        setCallerReference(hostedZone.callerReference());
        setComment(hostedZone.config().comment());
        setPrivateZone(hostedZone.config().privateZone());
        setHostedZoneName(hostedZone.name());
        setResourceRecordSetCount(hostedZone.resourceRecordSetCount());

        LinkedService linkedService = hostedZone.linkedService();
        setDescription(linkedService.description());
        setServicePrincipal(linkedService.servicePrincipal());


        ListVpcAssociationAuthorizationsResponse vpcResponse = client.listVPCAssociationAuthorizations(
            r -> r.hostedZoneId(getHostedZoneId())
        );

        if (!vpcResponse.vpCs().isEmpty()) {
            VPC vpc = vpcResponse.vpCs().get(0);
            setVpcId(vpc.vpcId());
            setVpcRegion(vpc.vpcRegionAsString());
        }

        return true;
    }

    @Override
    public void create() {
        Route53Client client = createClient(Route53Client.class);

        CreateHostedZoneResponse response = client.createHostedZone(
            r -> r.name(getHostedZoneName())
                .callerReference(getCallerReference())
                .delegationSetId(getDelegationSetId())
                .hostedZoneConfig(
                    o -> o.comment(getComment())
                        .privateZone(getPrivateZone())
                )
                .vpc(
                    o -> o.vpcId(getVpcId())
                        .vpcRegion(getVpcRegion())
                )
        );

        HostedZone hostedZone = response.hostedZone();
        setHostedZoneId(hostedZone.id());
        setResourceRecordSetCount(hostedZone.resourceRecordSetCount());

        LinkedService linkedService = hostedZone.linkedService();
        setDescription(linkedService.description());
        setServicePrincipal(linkedService.servicePrincipal());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Route53Client client = createClient(Route53Client.class);

        if (changedProperties.contains("comment")) {
            client.updateHostedZoneComment(
                r -> r.id(getHostedZoneId())
                    .comment(getComment())
            );
        }

        if (changedProperties.contains("vpc-id") || changedProperties.contains("vpc-region")) {
            HostedZoneResource oldHostedZoneResource = (HostedZoneResource) current;

            if (!ObjectUtils.isBlank(oldHostedZoneResource.getVpcId())) {
                client.disassociateVPCFromHostedZone(
                    r -> r.hostedZoneId(getHostedZoneId())
                        .vpc(
                            o -> o.vpcId(oldHostedZoneResource.getVpcId())
                                .vpcRegion(oldHostedZoneResource.getVpcRegion())
                        )
                );
            }

            client.associateVPCWithHostedZone(
                r -> r.hostedZoneId(getHostedZoneId())
                    .vpc(
                        o -> o.vpcId(getVpcId())
                            .vpcRegion(getVpcRegion())
                    )
            );
        }
    }

    @Override
    public void delete() {
        Route53Client client = createClient(Route53Client.class);

        client.deleteHostedZone(
            r -> r.id(getHostedZoneId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("hosted zone");

        if (!ObjectUtils.isBlank(getHostedZoneName())) {
            sb.append(" [ ").append(getHostedZoneName()).append(" ]");
        }

        if (!ObjectUtils.isBlank(getHostedZoneId())) {
            sb.append(" - ").append(getHostedZoneId());
        }

        return sb.toString();
    }
}
