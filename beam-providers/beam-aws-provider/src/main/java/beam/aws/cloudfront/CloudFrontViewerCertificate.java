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
        setCloudfrontDefaultCertificate(true);
        setMinimumProtocolVersion("TLSv1");
    }

    public CloudFrontViewerCertificate(ViewerCertificate viewerCertificate) {
        setCloudfrontDefaultCertificate(viewerCertificate.cloudFrontDefaultCertificate());
        setAcmCertificateArn(viewerCertificate.acmCertificateArn());
        setIamCertificateId(viewerCertificate.iamCertificateId());
        setMinimumProtocolVersion(viewerCertificate.minimumProtocolVersionAsString());
        setSslSupportMethod(viewerCertificate.sslSupportMethodAsString());
    }

    /**
     * Use the default CloudFront SSL certificate (i.e. ``*.cloudfront.net``).
     */
    @ResourceDiffProperty(updatable = true)
    public boolean getCloudfrontDefaultCertificate() {
        return cloudfrontDefaultCertificate;
    }

    public void setCloudfrontDefaultCertificate(boolean cloudfrontDefaultCertificate) {
        this.cloudfrontDefaultCertificate = cloudfrontDefaultCertificate;
    }

    /**
     * ARN for an ACM generated certificate.
     */
    @ResourceDiffProperty(updatable = true)
    public String getAcmCertificateArn() {
        return acmCertificateArn;
    }

    public void setAcmCertificateArn(String acmCertificateArn) {
        this.acmCertificateArn = acmCertificateArn;
    }

    /**
     * ID for certificated uploaded to IAM.
     */
    @ResourceDiffProperty(updatable = true)
    public String getIamCertificateId() {
        return iamCertificateId;
    }

    public void setIamCertificateId(String iamCertificateId) {
        this.iamCertificateId = iamCertificateId;
    }

    /**
     * Minimum SSL protocol. Valid valies are ``SSLv3``, ``TLSv1``, ``TLSv1_2016``, ``TLSv1.1_2016``, ``TLSv1.2_2018``.
     */
    @ResourceDiffProperty(updatable = true)
    public String getMinimumProtocolVersion() {
        return minimumProtocolVersion;
    }

    public void setMinimumProtocolVersion(String minimumProtocolVersion) {
        this.minimumProtocolVersion = minimumProtocolVersion;
    }

    /**
     * Whether CloudFront uses a dedicated IP or SNI for serving SSL traffic. Valid values are ``vip`` or ``sni-only``. There is a significant additional monthly charge for ``vip`.
     */
    @ResourceDiffProperty(updatable = true)
    public String getSslSupportMethod() {
        if (getCloudfrontDefaultCertificate())  {
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
            .cloudFrontDefaultCertificate(getCloudfrontDefaultCertificate())
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
