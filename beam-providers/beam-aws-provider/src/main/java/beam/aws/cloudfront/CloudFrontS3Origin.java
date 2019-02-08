package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;

public class CloudFrontS3Origin extends Diffable {

    private String originAccessIdentity;

    public String getOriginAccessIdentity() {
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
