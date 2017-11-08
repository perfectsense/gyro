package beam.openstack.config;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.diff.ResourceChange;
import beam.openstack.OpenStackCloud;
import com.google.common.collect.Lists;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SwiftContainerResource extends OpenStackResource<Container> {

    private String name;
    private boolean publicAccessible;
    private List<SwiftObjectResource> swiftObjects;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublicAccessible() {
        return publicAccessible;
    }

    public void setPublicAccessible(boolean publicAccessible) {
        this.publicAccessible = publicAccessible;
    }

    public List<SwiftObjectResource> getSwiftObjects() {
        if (swiftObjects == null) {
            swiftObjects = new ArrayList<>();
        }

        return swiftObjects;
    }

    public void setSwiftObjects(List<SwiftObjectResource> swiftObjects) {
        this.swiftObjects = swiftObjects;
    }

    @Override
    public List<?> diffIds() {
        return Lists.newArrayList(getName());
    }

    @Override
    public void init(OpenStackCloud cloud, BeamResourceFilter filter, Container container) {
        setName(container.getName());
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getSwiftObjects());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<OpenStackCloud, Container> current) throws Exception {
        SwiftContainerResource currentContainer = (SwiftContainerResource) current;

        update.update(currentContainer.getSwiftObjects(), getSwiftObjects());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getSwiftObjects());
    }

    @Override
    public void create(OpenStackCloud cloud) {
        CloudFilesApi cloudFilesApi = cloud.createCloudFilesApi();
        ContainerApi containerApi = cloudFilesApi.getContainerApi(getRegion());

        containerApi.create(getName());
    }

    @Override
    public void update(OpenStackCloud cloud, BeamResource<OpenStackCloud, Container> current, Set<String> changedProperties) {

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
        return "cloud files container " + getName();
    }
}