package gyro.aws.cloudfront;

import gyro.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;

public class CloudFrontLogging extends Diffable {

    private Boolean enabled;
    private String bucket;
    private String bucketPrefix;
    private Boolean includeCookies;

    public CloudFrontLogging() {
        setEnabled(false);
        setIncludeCookies(false);
        setBucket("");
        setBucketPrefix("");
    }

    public CloudFrontLogging(LoggingConfig loggingConfig) {
        setBucket(loggingConfig.bucket());
        setBucketPrefix(loggingConfig.prefix());
        setEnabled(loggingConfig.enabled());
        setIncludeCookies(loggingConfig.includeCookies());
    }

    /**
     * Enable or disable logging for this distribution.
     */
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Name of bucket to save access logs.
     */
    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Directory within bucket ot save access logs.
     */
    public String getBucketPrefix() {
        return bucketPrefix;
    }

    public void setBucketPrefix(String bucketPrefix) {
        this.bucketPrefix = bucketPrefix;
    }

    /**
     * Whether to include cookies logs.
     */
    public Boolean getIncludeCookies() {
        return includeCookies;
    }

    public void setIncludeCookies(Boolean includeCookies) {
        this.includeCookies = includeCookies;
    }

    public LoggingConfig toLoggingConfig() {
        return LoggingConfig.builder()
            .bucket(getBucket())
            .prefix(getBucketPrefix())
            .includeCookies(getIncludeCookies())
            .enabled(getEnabled()).build();
    }

    @Override
    public String primaryKey() {
        return "logging";
    }

    @Override
    public String toDisplayString() {
        return String.format("logging config - bucket: %s, prefix: %s", getBucket(), getBucketPrefix());
    }
}
