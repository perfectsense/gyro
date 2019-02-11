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

    public void setCloudfrontDefaultCertificate(boolean cloudfrontDefaultCertificate) {
        this.cloudfrontDefaultCertificate = cloudfrontDefaultCertificate;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAcmCertificateArn() {
        return acmCertificateArn;
    }

    public void setAcmCertificateArn(String acmCertificateArn) {
        this.acmCertificateArn = acmCertificateArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getIamCertificateId() {
        return iamCertificateId;
    }

    public void setIamCertificateId(String iamCertificateId) {
        this.iamCertificateId = iamCertificateId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getMinimumProtocolVersion() {
        if (!isCloudfrontDefaultCertificate() && minimumProtocolVersion == null) {
            minimumProtocolVersion = "TLSv1_2016";
        }

        return minimumProtocolVersion;
    }

    public void setMinimumProtocolVersion(String minimumProtocolVersion) {
        this.minimumProtocolVersion = minimumProtocolVersion;
    }

    @ResourceDiffProperty(updatable = true)
    public String getSslSupportMethod() {
        if (isCloudfrontDefaultCertificate())  {
            return null;
        } else if (sslSupportMethod == null) {
            return "sni-only";
        }

        return sslSupportMethod;
    }

    public void setSslSupportMethod(String sslSupportMethod) {
        this.sslSupportMethod = sslSupportMethod;
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
