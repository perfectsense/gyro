package beam.aws.cloudfront;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponses;
import software.amazon.awssdk.services.cloudfront.model.Distribution;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.NoSuchDistributionException;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private CloudFrontViewerCertificate viewerCertificate;
    private String waf;
    private String domainName;
    private List<CloudFrontCustomErrorResponse> customErrorResponse;
    private CloudFrontGeoRestriction geoRestriction;

    public CloudFrontResource() {
        setEnabled(true);
        setIpv6Enabled(false);
        setHttpVersion("http1.1");
        setLogging(new CloudFrontLogging());
        setViewerCertificate(new CloudFrontViewerCertificate());
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

        //Collections.sort(cnames);

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

    @ResourceDiffProperty(updatable = true)
    public CloudFrontCacheBehavior getDefaultCacheBehavior() {
        return defaultCacheBehavior;
    }

    public void setDefaultCacheBehavior(CloudFrontCacheBehavior defaultCacheBehavior) {
        this.defaultCacheBehavior = defaultCacheBehavior;

        defaultCacheBehavior.setPathPattern("*");
    }

    @ResourceDiffProperty(updatable = true)
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

    @ResourceDiffProperty(updatable = true)
    public CloudFrontViewerCertificate getViewerCertificate() {
        return viewerCertificate;
    }

    public void setViewerCertificate(CloudFrontViewerCertificate viewerCertificate) {
        this.viewerCertificate = viewerCertificate;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getWaf() {
        if (waf == null) {
            return "";
        }

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

    @ResourceDiffProperty(updatable = true, nullable = true)
    public CloudFrontGeoRestriction getGeoRestriction() {
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

        //applyTags(client);
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        client.updateDistribution(r -> r.distributionConfig(distributionConfig())
            .id(getId())
            .ifMatch(getEtag()));
    }

    @Override
    public void delete() {
        CloudFrontClient client = createClient(CloudFrontClient.class, "us-east-1", "https://cloudfront.amazonaws.com");

        if (isEnabled()) {
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

        builder.enabled(isEnabled())
            .comment(getComment())
            .httpVersion(getHttpVersion())
            .priceClass(getPriceClass())
            .defaultRootObject(getDefaultRootObject())
            .isIPV6Enabled(isIpv6Enabled())
            .webACLId(getWaf())
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
