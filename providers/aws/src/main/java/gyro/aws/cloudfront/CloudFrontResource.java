package gyro.aws.cloudfront;

import gyro.aws.AwsResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponses;
import software.amazon.awssdk.services.cloudfront.model.Distribution;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.cloudfront.model.NoSuchDistributionException;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.Tag;
import software.amazon.awssdk.services.cloudfront.model.Tags;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create a CloudFront distribution.
 *
 * .. code-block:: gyro
 *
 *    aws::cloudfront cloudfront-example
 *        name: "static asset cache"
 *        enabled: true
 *        ipv6-enabled: false
 *
 *        origin
 *            id: "S3-my-bucket"
 *            domain-name: "my-bucket.s3.us-east-1.amazonaws.com"
 *        end
 *
 *        default-cache-behavior
 *            target-origin-id: "S3-my-bucket-brightspot"
 *            viewer-protocol-policy: "allow-all"
 *            allowed-methods: ["GET", "HEAD"]
 *            cached-methods: ["GET", "HEAD"]
 *            headers: ["Origin"]
 *        end
 *
 *        geo-restriction
 *            type: "whitelist"
 *            restrictions: ["US"]
 *        end
 *    end
 */
@ResourceName("cloudfront")
public class CloudFrontResource extends AwsResource {

    private boolean enabled;
    private String comment;
    private List<String> cnames;
    private String httpVersion;
    private String priceClass;
    private String defaultRootObject;
    private String etag;
    private String callerReference;
    private boolean isIpv6Enabled;
    private String webAclId;
    private Map<String, String> tags;
    private List<CloudFrontOrigin> origin;
    private List<CloudFrontCacheBehavior> behavior;
    private CloudFrontCacheBehavior defaultCacheBehavior;
    private CloudFrontViewerCertificate viewerCertificate;
    private CloudFrontLogging logging;
    private List<CloudFrontCustomErrorResponse> customErrorResponse;
    private CloudFrontGeoRestriction geoRestriction;

    // -- Read only
    private String id;
    private String arn;
    private String domainName;

    public CloudFrontResource() {
        setEnabled(true);
        setIpv6Enabled(false);
        setHttpVersion("http1.1");
        setLogging(new CloudFrontLogging());
        setViewerCertificate(new CloudFrontViewerCertificate());
    }

    /**
     * The id of this CloudFront distribution.
     *
     */
    @ResourceOutput
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The arn of this CloudFront distribution.
     *
     */
    @ResourceOutput
    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    /**
     * Enable or disable this distribution without deleting it.
     */
    @ResourceDiffProperty(updatable = true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * A comment for this distribution.
     */
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

    /**
     * CNAMES (aliases) for which this distribution will listen for.
     */
    @ResourceDiffProperty(updatable = true)
    public List<String> getCnames() {
        if (cnames == null) {
            cnames = new ArrayList<>();
        }

        //Collections.sort(cnames);

        return cnames;
    }

    public void setCnames(List<String> cnames) {
        this.cnames = cnames;
    }

    /**
     * The maximum http version that users can request on this distribution. Valid values are ``HTTP1_1`` or ``HTTP2``.
     */
    @ResourceDiffProperty(updatable = true)
    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * The maximum price you want to pay for CloudFront. Valid values are ``PriceClass_All``, ``PriceClass_200`` and ``PriceClass_100``. For information on pricing see `Price classes <https://aws.amazon.com/cloudfront/pricing/#On-demand_Pricing>`_.
     */
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

    /**
     * The object to request from the origin when a user requests the root URL (i.e. http://www.example.com/).
     */
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

    /**
     * Enable IPv6 support for this distribution.
     */
    @ResourceDiffProperty(updatable = true)
    public boolean getIpv6Enabled() {
        return isIpv6Enabled;
    }

    public void setIpv6Enabled(boolean ipv6Enabled) {
        isIpv6Enabled = ipv6Enabled;
    }

    /**
     * The Web ACL (WAF) ID to associate with this distribution.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getWebAclId() {
        if (webAclId == null) {
            return "";
        }

        return webAclId;
    }

    public void setWebAclId(String webAclId) {
        this.webAclId = webAclId;
    }

    /**
     * The domain name for this distribution (i.e. ``abc123893.cloudfront.net``).
     *
     */
    @ResourceOutput("abc123.cloudfront.net")
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * A map of tags to apply to this distribution.
     */
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

    /**
     * List of origins for this distribution.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontOrigin
     */
    public List<CloudFrontOrigin> getOrigin() {
        if (origin == null) {
            origin = new ArrayList<>();
        }

        return origin;
    }

    public void setOrigin(List<CloudFrontOrigin> origin) {
        this.origin = origin;
    }

    /**
     * List of cache behaviors for this distribution.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontCacheBehavior
     */
    public List<CloudFrontCacheBehavior> getBehavior() {
        if (behavior == null) {
            behavior = new ArrayList<>();
        }

        return behavior;
    }

    public void setBehavior(List<CloudFrontCacheBehavior> behavior) {
        this.behavior = behavior;
    }

    /**
     * The default cache behavior for this distribution.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontCacheBehavior
     */
    @ResourceDiffProperty(updatable = true)
    public CloudFrontCacheBehavior getDefaultCacheBehavior() {
        return defaultCacheBehavior;
    }

    public void setDefaultCacheBehavior(CloudFrontCacheBehavior defaultCacheBehavior) {
        this.defaultCacheBehavior = defaultCacheBehavior;

        defaultCacheBehavior.setPathPattern("*");
    }

    /**
     * SSL certificate configuration.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontViewerCertificate
     */
    @ResourceDiffProperty(updatable = true)
    public CloudFrontViewerCertificate getViewerCertificate() {
        return viewerCertificate;
    }

    public void setViewerCertificate(CloudFrontViewerCertificate viewerCertificate) {
        this.viewerCertificate = viewerCertificate;
    }

    /**
     * Configure logging access logs to S3.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontLogging
     */
    @ResourceDiffProperty(updatable = true)
    public CloudFrontLogging getLogging() {
        return logging;
    }

    public void setLogging(CloudFrontLogging logging) {
        this.logging = logging;
    }

    /**
     * Replace HTTP codes with custom error responses as well as define cache TTLs for error responses.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontCustomErrorResponse
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<CloudFrontCustomErrorResponse> getCustomErrorResponse() {
        if (customErrorResponse == null) {
            customErrorResponse = new ArrayList<>();
        }

        return customErrorResponse;
    }

    public void setCustomErrorResponse(List<CloudFrontCustomErrorResponse> customErrorResponses) {
        this.customErrorResponse = customErrorResponses;
    }

    /**
     * Restrict or allow access to this distribution by country.
     *
     * @subresource gyro.aws.cloudfront.CloudFrontGeoRestriction
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public CloudFrontGeoRestriction getGeoRestriction() {
        if (geoRestriction == null) {
            return new CloudFrontGeoRestriction();
        }

        return geoRestriction;
    }

    public void setGeoRestriction(CloudFrontGeoRestriction geoRestriction) {
        this.geoRestriction = geoRestriction;
    }

    @Override
    public boolean refresh() {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        try {
            GetDistributionResponse response = client.getDistribution(r -> r.id(getId()));

            Distribution distribution = response.distribution();
            DistributionConfig config = distribution.distributionConfig();

            setEnabled(config.enabled());
            setComment(config.comment());
            setCnames(config.aliases().items());
            setHttpVersion(config.httpVersionAsString());
            setPriceClass(config.priceClassAsString());
            setCallerReference(config.callerReference());
            setIpv6Enabled(config.isIPV6Enabled());
            setDomainName(distribution.domainName());
            setEtag(response.eTag());

            if (config.origins() != null) {
                for (Origin origin : config.origins().items()) {
                    CloudFrontOrigin originResource = new CloudFrontOrigin(origin);
                    getOrigin().add(originResource);
                }
            }

            if (config.cacheBehaviors() != null) {
                for (CacheBehavior behavior : config.cacheBehaviors().items()) {
                    CloudFrontCacheBehavior cacheBehavior = new CloudFrontCacheBehavior(behavior);
                    getBehavior().add(cacheBehavior);
                }
            }

            CloudFrontCacheBehavior defaultCacheBehavior = new CloudFrontCacheBehavior(config.defaultCacheBehavior());
            setDefaultCacheBehavior(defaultCacheBehavior);

            CloudFrontLogging logging = config.logging() == null ? new CloudFrontLogging() : new CloudFrontLogging(config.logging());
            setLogging(logging);

            CloudFrontViewerCertificate viewerCertificate = config.viewerCertificate() == null
                ? new CloudFrontViewerCertificate() : new CloudFrontViewerCertificate(config.viewerCertificate());
            setViewerCertificate(viewerCertificate);

            CloudFrontGeoRestriction geoRestriction = new CloudFrontGeoRestriction(config.restrictions().geoRestriction());
            setGeoRestriction(geoRestriction);

            for (CustomErrorResponse errorResponse : config.customErrorResponses().items()) {
                CloudFrontCustomErrorResponse customErrorResponse = new CloudFrontCustomErrorResponse(errorResponse);
                getCustomErrorResponse().add(customErrorResponse);
            }

            ListTagsForResourceResponse tagsForResource = client.listTagsForResource(r -> r.resource(getArn()));
            getTags().clear();
            for (Tag tag: tagsForResource.tags().items()) {
                getTags().put(tag.key(), tag.value());
            }
        } catch (NoSuchDistributionException ex) {
            return false;
        }

        return true;
    }

    @Override
    public void create() {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        CreateDistributionResponse response = client.createDistribution(c -> c.distributionConfig(distributionConfig()));
        setId(response.distribution().id());
        setArn(response.distribution().arn());
        setDomainName(response.distribution().domainName());

        applyTags(client);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        client.updateDistribution(r -> r.distributionConfig(distributionConfig())
            .id(getId())
            .ifMatch(getEtag()));

        if (changedProperties.contains("tags")) {
            applyTags(client);
        }
    }

    @Override
    public void delete() {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        if (getEnabled()) {
            setEnabled(false);

            client.updateDistribution(r -> r.distributionConfig(distributionConfig())
                .id(getId())
                .ifMatch(getEtag()));

            boolean deploying = true;
            do {
                GetDistributionResponse response = client.getDistribution(r -> r.id(getId()));
                setEtag(response.eTag());

                if (response.distribution().status().equals("Deployed")) {
                    deploying = false;
                } else {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        throw new BeamException(ie.getMessage());
                    }
                }
            } while (deploying);
        }

        client.deleteDistribution(r -> r.id(getId()).ifMatch(getEtag()));
    }

    @Override
    public String toDisplayString() {
        return "cloudfront";
    }

    private void applyTags(CloudFrontClient client) {
        List<Tag> tags = new ArrayList<>();
        for (String key : getTags().keySet()) {
            Tag tag = Tag.builder().key(key).value(getTags().get(key)).build();
            tags.add(tag);
        }

        client.tagResource(r -> r.tags(Tags.builder().items(tags).build()).resource(getArn()));
    }

    private DistributionConfig distributionConfig() {
        DistributionConfig.Builder builder = DistributionConfig.builder();

        List<CustomErrorResponse> errorResponses = getCustomErrorResponse()
            .stream()
            .map(e -> e.toCustomErrorResponse())
            .collect(Collectors.toList());

        CustomErrorResponses customErrorResponses = CustomErrorResponses.builder()
            .items(errorResponses)
            .quantity(errorResponses.size())
            .build();

        List<CacheBehavior> behaviors = getBehavior()
            .stream()
            .map(c -> c.toCachBehavior())
            .collect(Collectors.toList());

        CacheBehaviors cacheBehaviors = CacheBehaviors.builder()
            .items(behaviors)
            .quantity(behaviors.size())
            .build();

        List<Origin> origin = getOrigin()
            .stream()
            .map(o -> o.toOrigin())
            .collect(Collectors.toList());

        Origins origins = Origins.builder()
            .items(origin)
            .quantity(origin.size())
            .build();

        CloudFrontViewerCertificate viewerCertificate = getViewerCertificate();
        if (viewerCertificate == null) {
            viewerCertificate = new CloudFrontViewerCertificate();
            viewerCertificate.setCloudfrontDefaultCertificate(true);
        }

        CloudFrontLogging logging = getLogging();
        if (logging == null) {
            logging = new CloudFrontLogging();
        }

        CloudFrontCacheBehavior defaultCacheBehavior = getDefaultCacheBehavior();
        if (defaultCacheBehavior == null) {
            defaultCacheBehavior = new CloudFrontCacheBehavior();
        }

        builder.enabled(getEnabled())
            .comment(getComment())
            .httpVersion(getHttpVersion())
            .priceClass(getPriceClass())
            .defaultRootObject(getDefaultRootObject())
            .isIPV6Enabled(getIpv6Enabled())
            .webACLId(getWebAclId())
            .aliases(a -> a.items(getCnames()).quantity(getCnames().size()))
            .restrictions(getGeoRestriction().toRestrictions())
            .customErrorResponses(customErrorResponses)
            .defaultCacheBehavior(defaultCacheBehavior.toDefaultCacheBehavior())
            .cacheBehaviors(cacheBehaviors)
            .origins(origins)
            .logging(logging.toLoggingConfig())
            .viewerCertificate(viewerCertificate.toViewerCertificate())
            .callerReference(getCallerReference() != null ? getCallerReference() : Long.toString(new Date().getTime()));

        return builder.build();
    }

}
