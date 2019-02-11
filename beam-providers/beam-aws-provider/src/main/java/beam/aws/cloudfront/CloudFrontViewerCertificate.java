package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.ViewerCertificate;

public class CloudFrontViewerCertificate extends Diffable {

    private boolean cloudfrontDefaultCertificate;
    private String acmCertificateArn;
    private String iamCertificateId;
    private String minimumProtocolVersion;
    private String sslSupportMethod;

    public boolean isCloudfrontDefaultCertificate() {
        return cloudfrontDefaultCertificate;
    }

    public CloudFrontViewerCertificate setCloudfrontDefaultCertificate(boolean cloudfrontDefaultCertificate) {
        this.cloudfrontDefaultCertificate = cloudfrontDefaultCertificate;
        return this;
    }

    public String getAcmCertificateArn() {
        return acmCertificateArn;
    }

    public CloudFrontViewerCertificate setAcmCertificateArn(String acmCertificateArn) {
        this.acmCertificateArn = acmCertificateArn;
        return this;
    }

    public String getIamCertificateId() {
        return iamCertificateId;
    }

    public CloudFrontViewerCertificate setIamCertificateId(String iamCertificateId) {
        this.iamCertificateId = iamCertificateId;
        return this;
    }

    public String getMinimumProtocolVersion() {
        return minimumProtocolVersion;
    }

    public CloudFrontViewerCertificate setMinimumProtocolVersion(String minimumProtocolVersion) {
        this.minimumProtocolVersion = minimumProtocolVersion;
        return this;
    }

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
