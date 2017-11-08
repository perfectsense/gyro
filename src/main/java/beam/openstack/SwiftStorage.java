package beam.openstack;

import beam.BeamStorage;
import com.google.common.collect.HashMultimap;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.openstack.swift.v1.options.PutOptions;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import java.io.IOException;
import java.io.InputStream;

public class SwiftStorage extends BeamStorage {

    private OpenStackCloud cloud;
    private String container;

    public SwiftStorage(OpenStackCloud cloud, String container) {
        this.cloud = cloud;
        this.container = container;
    }

    @Override
    public InputStream get(String path, String region) throws IOException {
        CloudFilesApi api = cloud.createCloudFilesApi();

        if (region == null) {
            region = "ORD";
        }

        ObjectApi objectApi = api.getObjectApi(region, container);
        SwiftObject object = objectApi.get(path);

        if (object != null) {
            return object.getPayload().openStream();
        }

        return null;
    }

    @Override
    public InputStream get(String path) throws IOException {
        return get(path, cloud.getDefaultRegion());
    }

    @Override
    public void put(String region, String path, InputStream content, String contentType, long length) throws IOException {
        CloudFilesApi api = cloud.createCloudFilesApi();

        if (region == null) {
            region = "ORD";
        }

        ContainerApi containerApi = api.getContainerApi(region);
        ObjectApi objectApi = api.getObjectApi(region, container);

        if (content != null) {
            if (containerApi.get(container) == null) {
                containerApi.create(container);
            }

            HashMultimap<String, String> headers = HashMultimap.create();
            if (contentType != null) {
                headers.put("Content-Type", contentType);
            } else {
                headers.put("Content-Type", "text/plain;charset=UTF-8");
            }

            headers.put("Content-Length", String.valueOf(length));

            PutOptions options = PutOptions.Builder.headers(headers);
            Payload payload = Payloads.newInputStreamPayload(content);

            objectApi.put(path, payload, options);
        } else if (containerApi.get(container) != null) {
            objectApi.delete(path);
        }
    }

    @Override
    public boolean doesExist(String region) {
        CloudFilesApi api = cloud.createCloudFilesApi();

        if (region == null) {
            region = "ORD";
        }

        ContainerApi containerApi = api.getContainerApi(region.toLowerCase());

        return containerApi.get(container) != null;
    }

}