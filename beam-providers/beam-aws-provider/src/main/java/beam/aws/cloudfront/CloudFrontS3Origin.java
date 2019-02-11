package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;

public class CloudFrontS3Origin extends Diffable {

    private String originAccessIdentity;

    public CloudFrontS3Origin() {
    }

    public CloudFrontS3Origin(S3OriginConfig s3OriginConfig) {
        setOriginAccessIdentity(s3OriginConfig.originAccessIdentity());
    }

    public String getOriginAccessIdentity() {
        if (originAccessIdentity == null) {
            return "";
        }

        return originAccessIdentity;
    }

    public void setOriginAccessIdentity(String originAccessIdentity) {
        this.originAccessIdentity = originAccessIdentity;
    }

    public S3OriginConfig toS3OriginConfig() {
        return S3OriginConfig.builder()
            .originAccessIdentity(getOriginAccessIdentity())
            .build();
    }

    @Override
    public String primaryKey() {
        return "s3-origin";
    }

    @Override
    public String toDisplayString() {
        return "s3 origin";
    }
}
