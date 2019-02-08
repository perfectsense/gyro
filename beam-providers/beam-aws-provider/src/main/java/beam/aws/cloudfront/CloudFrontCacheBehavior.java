package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.TrustedSigners;

import java.util.List;

public class CloudFrontCacheBehavior extends Diffable {

    private String targetOriginId;
    private String pathPattern;
    private String viewerProtocolPolicy;
    private Long minTtl;
    private List<String> allowedMethods;
    private List<String> cachedMethods;
    private List<String> headers;
    private String forwardCookies;
    private List<String> cookies;
    private boolean smoothStreaming;
    private Long defaultTtl;
    private Long maxTtl;
    private boolean compress;
    private boolean queryString;
    private List<String> queryStringCacheKeys;
    private List<String> trustedSigners;
    private String fieldLevelEncryptionId;

    public String getTargetOriginId() {
        return targetOriginId;
    }

    public void setTargetOriginId(String targetOriginId) {
        this.targetOriginId = targetOriginId;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getViewerProtocolPolicy() {
        return viewerProtocolPolicy;
    }

    public void setViewerProtocolPolicy(String viewerProtocolPolicy) {
        this.viewerProtocolPolicy = viewerProtocolPolicy;
    }

    public Long getMinTtl() {
        return minTtl;
    }

    public void setMinTtl(Long minTtl) {
        this.minTtl = minTtl;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getCachedMethods() {
        return cachedMethods;
    }

    public void setCachedMethods(List<String> cachedMethods) {
        this.cachedMethods = cachedMethods;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public String getForwardCookies() {
        return forwardCookies;
    }

    public void setForwardCookies(String forwardCookies) {
        this.forwardCookies = forwardCookies;
    }

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public boolean isSmoothStreaming() {
        return smoothStreaming;
    }

    public void setSmoothStreaming(boolean smoothStreaming) {
        this.smoothStreaming = smoothStreaming;
    }

    public Long getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Long getMaxTtl() {
        return maxTtl;
    }

    public void setMaxTtl(Long maxTtl) {
        this.maxTtl = maxTtl;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public boolean isQueryString() {
        return queryString;
    }

    public void setQueryString(boolean queryString) {
        this.queryString = queryString;
    }

    public List<String> getQueryStringCacheKeys() {
        return queryStringCacheKeys;
    }

    public void setQueryStringCacheKeys(List<String> queryStringCacheKeys) {
        this.queryStringCacheKeys = queryStringCacheKeys;
    }

    public List<String> getTrustedSigners() {
        return trustedSigners;
    }

    public void setTrustedSigners(List<String> trustedSigners) {
        this.trustedSigners = trustedSigners;
    }

    public String getFieldLevelEncryptionId() {
        return fieldLevelEncryptionId;
    }

    public void setFieldLevelEncryptionId(String fieldLevelEncryptionId) {
        this.fieldLevelEncryptionId = fieldLevelEncryptionId;
    }

    public DefaultCacheBehavior toDefaultCacheBehavior() {
        ForwardedValues forwardedValues = ForwardedValues.builder()
            .headers(h -> h.items(getHeaders()).quantity(getHeaders().size()))
            .cookies(c -> c.forward(getForwardCookies()).whitelistedNames(w -> w.items(getCookies()).quantity(getCookies().size())))
            .queryString(isQueryString())
            .queryStringCacheKeys(q -> q.items(getQueryStringCacheKeys()).quantity(getQueryStringCacheKeys().size()))
            .build();

        TrustedSigners trustedSigners = TrustedSigners.builder()
            .items(getTrustedSigners())
            .quantity(getTrustedSigners().size())
            .enabled(!getTrustedSigners().isEmpty())
            .build();

        return DefaultCacheBehavior.builder()
            .allowedMethods(am -> am.itemsWithStrings(getAllowedMethods()))
            .defaultTTL(getDefaultTtl())
            .maxTTL(getMaxTtl())
            .minTTL(getMinTtl())
            .smoothStreaming(isSmoothStreaming())
            .targetOriginId(getTargetOriginId())
            .forwardedValues(forwardedValues)
            .trustedSigners(trustedSigners)
            .viewerProtocolPolicy(getViewerProtocolPolicy())
            .fieldLevelEncryptionId(getFieldLevelEncryptionId())
            .compress(isCompress())
            .build();
    }

    public CacheBehavior toCachBehavior() {
        ForwardedValues forwardedValues = ForwardedValues.builder()
            .headers(h -> h.items(getHeaders()).quantity(getHeaders().size()))
            .cookies(c -> c.forward(getForwardCookies()).whitelistedNames(w -> w.items(getCookies()).quantity(getCookies().size())))
            .queryString(isQueryString())
            .queryStringCacheKeys(q -> q.items(getQueryStringCacheKeys()).quantity(getQueryStringCacheKeys().size()))
            .build();

        TrustedSigners trustedSigners = TrustedSigners.builder()
            .items(getTrustedSigners())
            .quantity(getTrustedSigners().size())
            .enabled(!getTrustedSigners().isEmpty())
            .build();

        return CacheBehavior.builder()
            .allowedMethods(am -> am.itemsWithStrings(getAllowedMethods()))
            .defaultTTL(getDefaultTtl())
            .maxTTL(getMaxTtl())
            .minTTL(getMinTtl())
            .smoothStreaming(isSmoothStreaming())
            .targetOriginId(getTargetOriginId())
            .pathPattern(getPathPattern())
            .forwardedValues(forwardedValues)
            .trustedSigners(trustedSigners)
            .viewerProtocolPolicy(getViewerProtocolPolicy())
            .fieldLevelEncryptionId(getFieldLevelEncryptionId())
            .compress(isCompress())
            .build();
    }

    @Override
    public String primaryKey() {
        return null;
    }

    @Override
    public String toDisplayString() {
        if (getPathPattern() != null && getPathPattern().equals("*")) {
            return "default cache behavior";
        }

        return "cache behavior";
    }
}
