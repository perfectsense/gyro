package beam.aws.cloudfront;

import beam.core.diff.Diffable;

import java.util.Map;

public class CloudFrontOrigin extends Diffable {

    private String domainName;
    private String originPath;
    private Map<String, String> customHeaders;
    private CloudFrontS3Origin s3origin;
    private CloudFrontCustomOrigin customOrigin;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getOriginPath() {
        return originPath;
    }

    public void setOriginPath(String originPath) {
        this.originPath = originPath;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public CloudFrontS3Origin getS3origin() {
        return s3origin;
    }

    public void setS3origin(CloudFrontS3Origin s3origin) {
        this.s3origin = s3origin;
    }

    public CloudFrontCustomOrigin getCustomOrigin() {
        return customOrigin;
    }

    public void setCustomOrigin(CloudFrontCustomOrigin customOrigin) {
        this.customOrigin = customOrigin;
    }

    @Override
    public String primaryKey() {
        return getDomainName();
    }

    @Override
    public String toDisplayString() {
        return "origin";
    }
}
