package beam.config;

import java.util.HashSet;
import java.util.Set;

@ConfigKey("cloud")
public abstract class CloudConfig extends Config {

    private String cloud;
    private Set<String> activeRegions;

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public Set<String> getActiveRegions() {
        if (activeRegions == null) {
            activeRegions = new HashSet<>();
        }

        return activeRegions;
    }

    public void setActiveRegions(Set<String> activeRegions) {
        this.activeRegions = activeRegions;
    }
}
