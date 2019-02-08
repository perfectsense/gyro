package beam.aws.cloudfront;

import beam.core.diff.Diffable;

public class CloudFrontS3Origin extends Diffable {

    private String originAccessIdentity;

    public String getOriginAccessIdentity() {
        return originAccessIdentity;
    }

    public void setOriginAccessIdentity(String originAccessIdentity) {
        this.originAccessIdentity = originAccessIdentity;
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
