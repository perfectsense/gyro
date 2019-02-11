package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import beam.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.TrustedSigners;

import java.util.ArrayList;
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


    public CloudFrontCacheBehavior() {
        setDefaultTtl(86400L);
        setMaxTtl(31536000L);
        setMinTtl(0L);
    }

    public CloudFrontCacheBehavior(CacheBehavior cacheBehavior) {
        setTargetOriginId(cacheBehavior.targetOriginId());
        setPathPattern(cacheBehavior.pathPattern());
        setViewerProtocolPolicy(cacheBehavior.viewerProtocolPolicyAsString());
        setTrustedSigners(cacheBehavior.trustedSigners().items());

        // -- TTLs
        setDefaultTtl(cacheBehavior.defaultTTL());
        setMinTtl(cacheBehavior.minTTL());
        setMaxTtl(cacheBehavior.maxTTL());

        // -- Forwarded Values
        setForwardCookies(cacheBehavior.forwardedValues().cookies().forwardAsString());
        if (!getForwardCookies().equals("none")) {
            setCookies(cacheBehavior.forwardedValues().cookies().whitelistedNames().items());
        }
        setHeaders(cacheBehavior.forwardedValues().headers().items());
        setQueryString(cacheBehavior.forwardedValues().queryString());
        setQueryStringCacheKeys(cacheBehavior.forwardedValues().queryStringCacheKeys().items());

        setAllowedMethods(cacheBehavior.allowedMethods().itemsAsStrings());
        setCompress(cacheBehavior.compress());
        setFieldLevelEncryptionId(cacheBehavior.fieldLevelEncryptionId());
        setSmoothStreaming(cacheBehavior.smoothStreaming());
    }

    public CloudFrontCacheBehavior(DefaultCacheBehavior cacheBehavior) {
        setTargetOriginId(cacheBehavior.targetOriginId());
        setPathPattern("*");
        setViewerProtocolPolicy(cacheBehavior.viewerProtocolPolicyAsString());
        setTrustedSigners(cacheBehavior.trustedSigners().items());

        // -- TTLs
        setDefaultTtl(cacheBehavior.defaultTTL());
        setMinTtl(cacheBehavior.minTTL());
        setMaxTtl(cacheBehavior.maxTTL());

        // -- Forwarded Values
        setForwardCookies(cacheBehavior.forwardedValues().cookies().forwardAsString());
        if (!getForwardCookies().equals("none")) {
            setCookies(cacheBehavior.forwardedValues().cookies().whitelistedNames().items());
        }
        setHeaders(cacheBehavior.forwardedValues().headers().items());
        setQueryString(cacheBehavior.forwardedValues().queryString());
        setQueryStringCacheKeys(cacheBehavior.forwardedValues().queryStringCacheKeys().items());

        setAllowedMethods(cacheBehavior.allowedMethods().itemsAsStrings());
        setCompress(cacheBehavior.compress());
        setFieldLevelEncryptionId(cacheBehavior.fieldLevelEncryptionId());
        setSmoothStreaming(cacheBehavior.smoothStreaming());
    }

    public String getTargetOriginId() {
        return targetOriginId;
    }

    public void setTargetOriginId(String targetOriginId) {
        this.targetOriginId = targetOriginId;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    @ResourceDiffProperty(updatable = true)
    public String getViewerProtocolPolicy() {
        return viewerProtocolPolicy;
    }

    public void setViewerProtocolPolicy(String viewerProtocolPolicy) {
        this.viewerProtocolPolicy = viewerProtocolPolicy;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getMinTtl() {
        return minTtl;
    }

    public void setMinTtl(Long minTtl) {
        this.minTtl = minTtl;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getAllowedMethods() {
        if (allowedMethods == null) {
            allowedMethods = new ArrayList<>();
        }

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

    @ResourceDiffProperty(updatable = true)
    public List<String> getHeaders() {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    @ResourceDiffProperty(updatable = true)
    public String getForwardCookies() {
        if (forwardCookies != null) {
            return forwardCookies.toLowerCase();
        }

        return "none";
    }

    public void setForwardCookies(String forwardCookies) {
        this.forwardCookies = forwardCookies;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getCookies() {
        if (cookies == null) {
            cookies = new ArrayList<>();
        }

        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isSmoothStreaming() {
        return smoothStreaming;
    }

    public void setSmoothStreaming(boolean smoothStreaming) {
        this.smoothStreaming = smoothStreaming;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    @ResourceDiffProperty(updatable = true)
    public Long getMaxTtl() {
        return maxTtl;
    }

    public void setMaxTtl(Long maxTtl) {
        this.maxTtl = maxTtl;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isQueryString() {
        return queryString;
    }

    public void setQueryString(boolean queryString) {
        this.queryString = queryString;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getQueryStringCacheKeys() {
        if (queryStringCacheKeys == null) {
            queryStringCacheKeys = new ArrayList<>();
        }

        return queryStringCacheKeys;
    }

    public void setQueryStringCacheKeys(List<String> queryStringCacheKeys) {
        this.queryStringCacheKeys = queryStringCacheKeys;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getTrustedSigners() {
        if (trustedSigners == null) {
            trustedSigners = new ArrayList<>();
        }

        return trustedSigners;
    }

    public void setTrustedSigners(List<String> trustedSigners) {
        this.trustedSigners = trustedSigners;
    }

    @ResourceDiffProperty(updatable = true)
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
            .allowedMethods(am -> am.itemsWithStrings(getAllowedMethods()).quantity(getAllowedMethods().size()))
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
            .allowedMethods(am -> am.itemsWithStrings(getAllowedMethods()).quantity(getAllowedMethods().size()))
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
        return getPathPattern();
    }

    @Override
    public String toDisplayString() {
        if (getPathPattern() != null && getPathPattern().equals("*")) {
            return "default cache behavior";
        }

        return "cache behavior";
    }
}
