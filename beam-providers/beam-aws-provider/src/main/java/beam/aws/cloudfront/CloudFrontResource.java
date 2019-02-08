package beam.aws.cloudfront;

import beam.aws.AwsResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ResourceName("cloudfront")
public class CloudFrontResource extends AwsResource {

    private String id;
    private String arn;
    private String name;
    private boolean enabled;
    private String comment;
    private List<String> cnames;
    private String httpVersion;
    private String priceClass;
    private String defaultRootObject;
    private CloudFrontLogging logging;
    private Map<String, String> tags;
    private List<CloudFrontOrigin> origin;
    private List<CloudFrontCacheBehavior> behavior;
    private CloudFrontCacheBehavior defaultCacheBehavior;
    private String etag;
    private String callerReference;
    private boolean isIpv6Enabled;
    private CloudFrontViewCertificate viewerCertificate;
    private String waf;
    private String domainName;
    private List<CloudFrontCustomErrorResponse> customErrorResponse;
    private CloudFrontGeoRestriction geoRestriction;

    public CloudFrontResource() {
        setEnabled(true);
        setIpv6Enabled(false);
        setHttpVersion("http1.1");
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @ResourceDiffProperty(updatable = true)
    public String getComment() {
        if (comment == null) {
            return "";
        }

        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getCnames() {
        if (cnames == null) {
            cnames = new ArrayList<>();
        }

        Collections.sort(cnames);

        return cnames;
    }

    public void setCnames(List<String> cnames) {
        this.cnames = cnames;
    }

    @ResourceDiffProperty(updatable = true)
    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    @ResourceDiffProperty(updatable = true)
    public String getPriceClass() {
        if (priceClass == null) {
            return "PriceClass_All";
        }

        return priceClass;
    }

    public void setPriceClass(String priceClass) {
        this.priceClass = priceClass;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDefaultRootObject() {
        if (defaultRootObject == null) {
            return "";
        }

        return defaultRootObject;
    }

    public void setDefaultRootObject(String defaultRootObject) {
        this.defaultRootObject = defaultRootObject;
    }

    public List<CloudFrontOrigin> getOrigin() {
        if (origin == null) {
            origin = new ArrayList<>();
        }

        return origin;
    }

    public void setOrigin(List<CloudFrontOrigin> origin) {
        this.origin = origin;
    }

    public List<CloudFrontCacheBehavior> getBehavior() {
        if (behavior == null) {
            behavior = new ArrayList<>();
        }

        return behavior;
    }

    public void setBehavior(List<CloudFrontCacheBehavior> behavior) {
        this.behavior = behavior;
    }

    public CloudFrontCacheBehavior getDefaultCacheBehavior() {
        return defaultCacheBehavior;
    }

    public void setDefaultCacheBehavior(CloudFrontCacheBehavior defaultCacheBehavior) {
        this.defaultCacheBehavior = defaultCacheBehavior;

        defaultCacheBehavior.setPathPattern("*");
    }

    public CloudFrontLogging getLogging() {
        return logging;
    }

    public void setLogging(CloudFrontLogging logging) {
        this.logging = logging;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getCallerReference() {
        return callerReference;
    }

    public void setCallerReference(String callerReference) {
        this.callerReference = callerReference;
    }

    @ResourceDiffProperty(updatable = true)
    public boolean isIpv6Enabled() {
        return isIpv6Enabled;
    }

    public void setIpv6Enabled(boolean ipv6Enabled) {
        isIpv6Enabled = ipv6Enabled;
    }

    public CloudFrontViewCertificate getViewerCertificate() {
        return viewerCertificate;
    }

    public void setViewerCertificate(CloudFrontViewCertificate viewerCertificate) {
        this.viewerCertificate = viewerCertificate;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getWaf() {
        return waf;
    }

    public void setWaf(String waf) {
        this.waf = waf;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<CloudFrontCustomErrorResponse> getCustomErrorResponse() {
        if (customErrorResponse == null) {
            customErrorResponse = new ArrayList<>();
        }

        return customErrorResponse;
    }

    public void setCustomErrorResponse(List<CloudFrontCustomErrorResponse> customErrorResponses) {
        this.customErrorResponse
            = customErrorResponses;
    }

    public CloudFrontGeoRestriction getGeoRestriction() {
        return geoRestriction;
    }

    public void setGeoRestriction(CloudFrontGeoRestriction geoRestriction) {
        this.geoRestriction = geoRestriction;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        return "cloudfront";
    }

}
