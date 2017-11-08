package beam.azure.config;

import beam.BeamResource;
import beam.BeamRuntime;
import beam.azure.AzureCloud;

public abstract class AzureResource<A> extends BeamResource<AzureCloud, A> {

    private String region;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getResourceGroup() {
        return String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), getRegion());
    }
}
