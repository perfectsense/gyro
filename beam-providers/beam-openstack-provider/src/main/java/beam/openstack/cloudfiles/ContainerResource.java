package beam.openstack.cloudfiles;

import beam.core.BeamCore;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import beam.openstack.OpenstackResource;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudfiles.v1.domain.CDNContainer;
import org.jclouds.rackspace.cloudfiles.v1.features.CDNApi;
import org.jclouds.rackspace.cloudfiles.v1.options.UpdateCDNContainerOptions;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates a container.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     openstack::container container-example
 *         container-name: "container-example"
 *         enable-cdn: false
 *         enable-cdn-logging: false
 *         enable-access-logging: true
 *     end
 */
@ResourceName("container")
public class ContainerResource extends OpenstackResource {

    private String containerName;
    private Boolean enableCdn;
    private Boolean enableAccessLogging;
    private Boolean enableCdnLogging;
    private Integer ttl;
    private Boolean enableStaticWebsite;
    private String staticWebsiteErrorPage;
    private String staticWebsiteIndexPage;

    //----Read only----//
    private String uri;
    private String streamingUri;
    private String sslUri;
    private String iosUri;

    /**
     * The name of the container. (Required)
     */
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    /**
     * Enable cdn on the container. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableCdn() {
        if (enableCdn == null) {
            enableCdn = false;
        }

        return enableCdn;
    }

    public void setEnableCdn(Boolean enableCdn) {
        this.enableCdn = enableCdn;
    }

    /**
     * Enable access logging on the container. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Boolean getEnableAccessLogging() {
        if (enableAccessLogging == null) {
            enableAccessLogging = false;
        }

        return enableAccessLogging;
    }

    public void setEnableAccessLogging(Boolean enableAccessLogging) {
        this.enableAccessLogging = enableAccessLogging;
    }

    /**
     * Enable cdn logging on the container. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableCdnLogging() {
        if (enableCdnLogging == null) {
            enableCdnLogging = false;
        }

        return enableCdnLogging;
    }

    public void setEnableCdnLogging(Boolean enableCdnLogging) {
        this.enableCdnLogging = enableCdnLogging;
    }

    /**
     * Set the ttl value. Required when param 'enable-cdn' is set to true.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getTtl() {
        if (ttl == null) {
            ttl = 0;
        }

        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    /**
     * Enable Static website configuration. Defaults to false.
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public Boolean getEnableStaticWebsite() {
        if (enableStaticWebsite == null) {
            enableStaticWebsite = false;
        }

        return enableStaticWebsite;
    }

    public void setEnableStaticWebsite(Boolean enableStaticWebsite) {
        this.enableStaticWebsite = enableStaticWebsite;
    }

    /**
     * Set the error page. Defaults to 'error.html'.
     */
    @ResourceDiffProperty(updatable = true)
    public String getStaticWebsiteErrorPage() {
        return staticWebsiteErrorPage;
    }

    public void setStaticWebsiteErrorPage(String staticWebsiteErrorPage) {
        this.staticWebsiteErrorPage = staticWebsiteErrorPage;
    }

    /**
     * Set the index page. Defaults to 'index.html'.
     */
    @ResourceDiffProperty(updatable = true)
    public String getStaticWebsiteIndexPage() {
        return staticWebsiteIndexPage;
    }

    public void setStaticWebsiteIndexPage(String staticWebsiteIndexPage) {
        this.staticWebsiteIndexPage = staticWebsiteIndexPage;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStreamingUri() {
        return streamingUri;
    }

    public void setStreamingUri(String streamingUri) {
        this.streamingUri = streamingUri;
    }

    public String getSslUri() {
        return sslUri;
    }

    public void setSslUri(String sslUri) {
        this.sslUri = sslUri;
    }

    public String getIosUri() {
        return iosUri;
    }

    public void setIosUri(String iosUri) {
        this.iosUri = iosUri;
    }

    @Override
    public boolean refresh() {
        CDNApi cdnClient = createClient(CDNApi.class);

        CDNContainer cdnContainer = cdnClient.get(getContainerName());

        setEnableCdn(cdnContainer.isEnabled());
        setEnableCdnLogging(getEnableCdn() && cdnContainer.isLogRetentionEnabled());
        setTtl(getEnableCdn() ? cdnContainer.getTtl() : 0);
        setUri(cdnContainer.getUri().toString());
        setStreamingUri(cdnContainer.getStreamingUri().toString());
        setIosUri(cdnContainer.getIosUri().toString());
        setSslUri(cdnContainer.getSslUri().toString());

        ContainerApi containerClient = createClient(ContainerApi.class);

        Container container = containerClient.get(getContainerName());

        loadStaticWebsiteConfig(container);

        setEnableAccessLogging(!getEnableCdn()
            && container.getMetadata().containsKey("access-log-delivery")
            && container.getMetadata().get("access-log-delivery").equals("true")
        );

        return true;
    }

    @Override
    public void create() {
        CDNApi cdnClient = createClient(CDNApi.class);

        ContainerApi containerClient = createClient(ContainerApi.class);

        validate();

        containerClient.create(getContainerName());

        try {
            if (getEnableCdn()) {
                saveCdnOptions(cdnClient);
            } else {
                cdnClient.disable(getContainerName());
            }

            if (getEnableAccessLogging()) {
                saveAccessLogging(containerClient);
            }

            if (getEnableStaticWebsite()) {
                saveStaticWebsite(containerClient);
            }
        } catch (Exception ex) {
            BeamCore.ui().write("\n@|red,red Error creating configs for container '%s'.|@", getContainerName());
            BeamCore.ui().write("\n@|red,red Error thrown - %s.|@", ex.getMessage());
            BeamCore.ui().write("\n@|red,red Retry to configure the additional configuration|@");
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        CDNApi cdnClient = createClient(CDNApi.class);

        ContainerApi containerClient = createClient(ContainerApi.class);

        validate();

        if (changedProperties.contains("enable-cdn") && !getEnableCdn()) {
            cdnClient.disable(getContainerName());
        } else if (getEnableCdn()) {
            if (changedProperties.contains("enable-cdn")
                || changedProperties.contains("enable-cdn-logging")
                || changedProperties.contains("ttl")) {

                saveCdnOptions(cdnClient);
            }
        }

        if (changedProperties.contains("enable-static-website")
            || changedProperties.contains("static-website-error-page")
            || changedProperties.contains("static-website-index-page")) {
            saveStaticWebsite(containerClient);
        }

        if (changedProperties.contains("enable-access-logging")) {
            saveAccessLogging(containerClient);
        }
    }

    @Override
    public void delete() {
        ContainerApi containerClient = createClient(ContainerApi.class);

        containerClient.deleteIfEmpty(getContainerName());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("container");

        if (!ObjectUtils.isBlank(getContainerName())) {
            sb.append(" - ").append(getContainerName());
        }

        return sb.toString();
    }

    @Override
    protected Class<? extends Closeable> getParentClientClass() {
        return CloudFilesApi.class;
    }

    private void saveAccessLogging(ContainerApi containerApi) {
        Map<String, String> metadata = new HashMap<>(containerApi.get(getContainerName()).getMetadata());

        metadata.put("access-log-delivery", getEnableAccessLogging() ? "true" : "false");

        containerApi.updateMetadata(getContainerName(), metadata);
    }

    private void saveStaticWebsite(ContainerApi containerApi) {
        Map<String, String> metadata = new HashMap<>(containerApi.get(getContainerName()).getMetadata());

        if (!getEnableStaticWebsite()) {
            Map<String, String> metadataRemove = new HashMap<>();
            metadataRemove.put("web-error", metadata.remove("web-error"));
            metadataRemove.put("web-index", metadata.remove("web-index"));

            containerApi.deleteMetadata(getContainerName(), metadataRemove);
        } else {
            metadata.put("web-error", !ObjectUtils.isBlank(getStaticWebsiteErrorPage()) ? getStaticWebsiteErrorPage() : "error.html");
            metadata.put("web-index", !ObjectUtils.isBlank(getStaticWebsiteIndexPage()) ? getStaticWebsiteIndexPage() : "index.html");

            containerApi.updateMetadata(getContainerName(), metadata);
        }
    }

    private void loadStaticWebsiteConfig(Container container) {
        setEnableStaticWebsite(container.getMetadata().containsKey("web-error"));

        if (getEnableStaticWebsite()) {
            setStaticWebsiteErrorPage(container.getMetadata().get("web-error"));
            setStaticWebsiteIndexPage(container.getMetadata().get("web-index"));
        }
    }

    private void saveCdnOptions(CDNApi cdnApi) {
        UpdateCDNContainerOptions options = UpdateCDNContainerOptions
            .Builder.enabled(true)
            .logRetention(getEnableCdnLogging())
            .ttl(getTtl());

        cdnApi.update(getContainerName(), options);
    }

    private void validate() {
        if (getEnableCdn()) {
            if (getEnableAccessLogging()) {
                throw new BeamException("The param 'enable-access-logging' cannot be set to true when the param 'enable-cdn' is set to true.");
            }

            if (getTtl() < 1) {
                throw new BeamException("The param 'ttl' has invalid value. Valid values [ Integer starting 1 ].");
            }
        } else {
            if (getEnableCdnLogging()) {
                throw new BeamException("The param 'enable-cdn-logging' cannot be set to true when the param 'enable-cdn' is set to false.");
            }

            if (getEnableStaticWebsite()) {
                throw new BeamException("The param 'enable-static-website' cannot be set to true when the param 'enable-cdn' is set to false.");
            }

            if (getTtl() != 0) {
                throw new BeamException("The param 'ttl' cannot be set when the param 'enable-cdn' is set to false.");
            }
        }
    }
}
