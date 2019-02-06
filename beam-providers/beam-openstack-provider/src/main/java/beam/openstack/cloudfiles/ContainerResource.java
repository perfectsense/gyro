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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    //----API clients----//
    private CloudFilesApi client;
    private ContainerApi containerApi;
    private CDNApi cdnApi;

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @ResourceDiffProperty(updatable = true)
    public Boolean getEnableCdn() {
        if (enableCdn == null) {
            enableCdn = true;
        }

        return enableCdn;
    }

    public void setEnableCdn(Boolean enableCdn) {
        this.enableCdn = enableCdn;
    }

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

    @ResourceDiffProperty(updatable = true)
    public String getStaticWebsiteErrorPage() {
        return staticWebsiteErrorPage;
    }

    public void setStaticWebsiteErrorPage(String staticWebsiteErrorPage) {
        this.staticWebsiteErrorPage = staticWebsiteErrorPage;
    }

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
        setupClients();

        Container container = containerApi.get(getContainerName());

        loadStaticWebsiteConfig(container);

        CDNContainer cdnContainer = cdnApi.get(getContainerName());

        setEnableCdn(cdnContainer.isEnabled());
        setEnableCdnLogging(getEnableCdn() && cdnContainer.isLogRetentionEnabled());
        setTtl(getEnableCdn() ? cdnContainer.getTtl() : 0);
        setUri(cdnContainer.getUri().toString());
        setStreamingUri(cdnContainer.getStreamingUri().toString());
        setIosUri(cdnContainer.getIosUri().toString());
        setSslUri(cdnContainer.getSslUri().toString());

        setEnableAccessLogging(!getEnableCdn()
            && container.getMetadata().containsKey("access-log-delivery")
            && container.getMetadata().get("access-log-delivery").equals("true")
        );

        return true;
    }

    @Override
    public void create() {
        setupClients();

        validate();

        containerApi.create(getContainerName());

        try {
            if (getEnableCdn()) {
                saveCdnOptions();
            } else {
                cdnApi.disable(getContainerName());
            }

            if (getEnableAccessLogging()) {
                saveAccessLogging();
            }

            if (getEnableStaticWebsite()) {
                saveStaticWebsite();
            }
        } catch (Exception ex) {
            BeamCore.ui().write("\n@|red,red Error creating configs for container '%s'.|@", getContainerName());
            BeamCore.ui().write("\n@|red,red Error thrown - %s.|@", ex.getMessage());
            BeamCore.ui().write("\n@|red,red Retry to configure the additional configuration|@");
        }
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        setupClients();

        validate();

        if (changedProperties.contains("enable-cdn") && !getEnableCdn()) {
            cdnApi.disable(getContainerName());
        } else if (getEnableCdn()) {
            if (changedProperties.contains("enable-cdn")
                || changedProperties.contains("enable-cdn-logging")
                || changedProperties.contains("ttl")) {

                saveCdnOptions();
            }
        }

        if (changedProperties.contains("enable-static-website")
            || changedProperties.contains("static-website-error-page")
            || changedProperties.contains("static-website-index-page")) {
            saveStaticWebsite();
        }

        if (changedProperties.contains("enable-access-logging")) {
            saveAccessLogging();
        }
    }

    @Override
    public void delete() {
        setupClients();

        containerApi.deleteIfEmpty(getContainerName());
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

    private void setupClients() {
        if (client == null) {
            client = createClient(CloudFilesApi.class);
        }

        if (containerApi == null) {
            containerApi = client.getContainerApi(getRegion());
        }

        if (cdnApi == null) {
            cdnApi = client.getCDNApi(getRegion());
        }
    }

    private void saveAccessLogging() {
        Map<String, String> metadata = new HashMap<>(containerApi.get(getContainerName()).getMetadata());

        metadata.put("access-log-delivery", getEnableAccessLogging() ? "true" : "false");

        containerApi.updateMetadata(getContainerName(), metadata);
    }

    private void saveStaticWebsite() {
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

    private void saveCdnOptions() {
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
