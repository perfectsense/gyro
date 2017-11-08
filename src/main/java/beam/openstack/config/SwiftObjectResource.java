package beam.openstack.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceDiffProperty;
import beam.openstack.OpenStackCloud;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.psddev.dari.util.IoUtils;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwiftObjectResource extends OpenStackResource<SwiftObject> {

    private static final String OBJECT_CONTENT_URL_KEY = "beam-object-content-url";

    private BeamReference containerRef;
    private String path;
    private String objectContentUrl;

    private Pattern pathRegex = Pattern.compile("cloudfiles:\\/\\/([^\\/]+)\\/([^\\/]+)\\/(.*)");

    public BeamReference getContainer() {
        return containerRef;
    }

    public void setContainer(BeamReference container) {
        this.containerRef = container;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @ResourceDiffProperty(updatable = true)
    public String getObjectContentUrl() {
        return objectContentUrl;
    }

    public void setObjectContentUrl(String objectContentUrl) {
        this.objectContentUrl = objectContentUrl;
    }

    @Override
    public List<?> diffIds() {
        return Arrays.asList(getContainer(), getPath());
    }

    @Override
    public BeamResource<OpenStackCloud, SwiftObject> findCurrent(OpenStackCloud cloud, BeamResourceFilter filter) {
        SwiftContainerResource container = (SwiftContainerResource) containerRef.resolve();

        CloudFilesApi api = cloud.createCloudFilesApi();
        ObjectApi objectApi = api.getObjectApi(container.getRegion(), container.getName());

        SwiftObject swiftObject = objectApi.getWithoutBody(getPath());
        if (swiftObject == null) {
            return null;
        }

        SwiftObjectResource current = new SwiftObjectResource();
        current.init(cloud, filter, swiftObject);

        return current;
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, SwiftObject cloudResource) {
        setPath(cloudResource.getName());

        Map<String, String> metadata = cloudResource.getMetadata();
        if (metadata != null) {
            setObjectContentUrl(metadata.get(OBJECT_CONTENT_URL_KEY));
        }
    }

    @Override
    public void create(OpenStackCloud cloud) {
        SwiftContainerResource container = (SwiftContainerResource) containerRef.resolve();

        String containerName = container.getName();

        CloudFilesApi api = cloud.createCloudFilesApi();
        ObjectApi objectApi = api.getObjectApi(container.getRegion(), containerName);

        Matcher matcher = pathRegex.matcher(getObjectContentUrl());
        if (matcher.find()) {
            String sourceRegion = matcher.group(1);
            String sourceContainer = matcher.group(2);
            String pathKey = matcher.group(3);

            if (container.getRegion().equals(sourceRegion)) {
                objectApi.copy(getPath(), sourceContainer, pathKey);
            } else {
                ObjectApi sourceObjectApi = api.getObjectApi(sourceRegion, sourceContainer);
                SwiftObject sourceObject = sourceObjectApi.get(pathKey);
                try {
                    File temp = File.createTempFile("container", "beam");
                    temp.deleteOnExit();

                    OutputStream output = new FileOutputStream(temp);
                    IoUtils.copy(sourceObject.getPayload().openStream(), output);

                    ByteSource byteSource = Files.asByteSource(temp);
                    Payload payload = Payloads.newByteSourcePayload(byteSource);

                    objectApi.put(getPath(), payload);
                } catch (Exception ex) {
                    throw new BeamException("Failed to copy object to container!");
                }
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put(OBJECT_CONTENT_URL_KEY, getObjectContentUrl());
            objectApi.updateMetadata(getPath(), metadata);
        }
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, SwiftObject> current, Set<String> changedProperties) {
        create(cloud);
    }

    @Override
    public void delete(OpenStackCloud cloud) {

    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public String toDisplayString() {
        return "cloud files object " + getPath();
    }
}
