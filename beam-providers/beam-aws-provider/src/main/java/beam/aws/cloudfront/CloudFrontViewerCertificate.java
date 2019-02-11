package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.cloudfront.model.ViewerCertificate;

public class CloudFrontViewerCertificate extends Diffable {

    private boolean cloudfrontDefaultCertificate;
    private String acmCertificateArn;
    private String iamCertificateId;
    private String minimumProtocolVersion;
    private String sslSupportMethod;

    public CloudFrontViewerCertificate() {
    }

    public CloudFrontViewerCertificate(ViewerCertificate viewerCertificate) {
        setCloudfrontDefaultCertificate(viewerCertificate.cloudFrontDefaultCertificate());
        setAcmCertificateArn(viewerCertificate.acmCertificateArn());
        setIamCertificateId(viewerCertificate.iamCertificateId());
        setMinimumProtocolVersion(viewerCertificate.minimumProtocolVersionAsString());
        setSslSupportMethod(viewerCertificate.sslSupportMethodAsString());
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isCloudfrontDefaultCertificate() {
        return cloudfrontDefaultCertificate;
    }

    public CloudFrontViewerCertificate setCloudfrontDefaultCertificate(boolean cloudfrontDefaultCertificate) {
        this.cloudfrontDefaultCertificate = cloudfrontDefaultCertificate;
        return this;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAcmCertificateArn() {
        return acmCertificateArn;
    }

    public CloudFrontViewerCertificate setAcmCertificateArn(String acmCertificateArn) {
        this.acmCertificateArn = acmCertificateArn;
        return this;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIamCertificateId() {
        return iamCertificateId;
    }

    public CloudFrontViewerCertificate setIamCertificateId(String iamCertificateId) {
        this.iamCertificateId = iamCertificateId;
        return this;
    }

    @ResourceDiffProperty(updatable = true)
    public String getMinimumProtocolVersion() {
        return minimumProtocolVersion;
    }

    public CloudFrontViewerCertificate setMinimumProtocolVersion(String minimumProtocolVersion) {
        this.minimumProtocolVersion = minimumProtocolVersion;
        return this;
    }

    @ResourceDiffProperty(updatable = true)
    public String getSslSupportMethod() {
        return sslSupportMethod;
    }

    public CloudFrontViewerCertificate setSslSupportMethod(String sslSupportMethod) {
        this.sslSupportMethod = sslSupportMethod;
        return this;
    }

    public ViewerCertificate toViewerCertificate() {
        return ViewerCertificate.builder()
            .acmCertificateArn(getAcmCertificateArn())
            .iamCertificateId(getIamCertificateId())
            .minimumProtocolVersion(getMinimumProtocolVersion())
            .sslSupportMethod(getSslSupportMethod())
            .cloudFrontDefaultCertificate(isCloudfrontDefaultCertificate())
            .build();
    }

    @Override
    public String primaryKey() {
        return "viewer-certificate";
    }

    @Override
    public String toDisplayString() {
        return "viewer certificate";
    }
}
