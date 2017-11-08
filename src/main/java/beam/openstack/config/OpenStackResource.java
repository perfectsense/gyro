package beam.openstack.config;

import beam.BeamResource;
import beam.openstack.OpenStackCloud;

public abstract class OpenStackResource<A> extends BeamResource<OpenStackCloud, A> {

    private String region;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}