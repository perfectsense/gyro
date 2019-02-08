package beam.aws.elbv2;

import beam.aws.AwsResource;
import beam.core.diff.Delete;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;

import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     certificate
 *         certificate-arn: "arn:aws:acm:us-east-2:acct-number:certificate/certificatearn"
 *     end
 */

@ResourceName(parent = "alb-listener", value = "certificate")
@ResourceName(parent = "nlb-listener", value = "certificate")
public class CertificateResource extends AwsResource {

    private String certificateArn;
    private Boolean isDefault;

    public CertificateResource() {

    }

    /**
     *  ARN of the certificate (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getCertificateArn() {
        return certificateArn;
    }

    public void setCertificateArn(String certificateArn) {
        this.certificateArn = certificateArn;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getListenerArn() {
        ListenerResource parent;

        if (parentResource() instanceof ApplicationLoadBalancerListenerResource) {
            parent = (ApplicationLoadBalancerListenerResource) parentResource();
        } else {
            parent = (ApplicationLoadBalancerListenerResource) parentResource();
        }

        if (parent != null) {
            return parent.getListenerArn();
        }

        return null;
    }

    @Override
    public String primaryKey() {
        return String.format("%s", getCertificateArn());
    }

    @Override
    public boolean refresh() {
        return true;
    }

    @Override
    public void create() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);

        client.addListenerCertificates(r -> r.certificates(toCertificate())
                                            .listenerArn(getListenerArn()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {}

    @Override
    public void delete() {
        if (parentResource().change() instanceof Delete) {
            return;
        }

        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.removeListenerCertificates(r -> r.certificates(toCertificate())
                .listenerArn(getListenerArn()));
    }

    @Override
    public String toDisplayString() {
        return "certificate " + getCertificateArn();
    }

    private Certificate toCertificate() {
        return Certificate.builder()
                .certificateArn(getCertificateArn())
                .build();
    }
}
