package beam;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.diff.Diff;

public abstract class BeamCloud {

    private Set<String> includedLayers;
    private Set<String> activeRegions;

    /**
     * Return the name of this cloud.
     *
     * @return Never {@code null}.
     */
    public abstract String getName();

    /**
     * Returns all instances in this cloud.
     *
     * @param cacheOk {@code true} if it's OK to return a cached list.
     * @return Never {@code null}.
     */
    public abstract List<? extends BeamInstance> getInstances(boolean cacheOk) throws Exception;

    /**
     * @param runtime Can't be {@code null}.
     * @return May be {@code null} to indicate no changes.
     */
    public abstract List<Diff<?, ?, ?>> findChanges(BeamRuntime runtime) throws Exception;

    /**
     * Finds the first reachable gateway in this cloud based on the given
     * {@code config}.
     *
     * @param runtime Can't be {@code null}.
     * @return May be {@code null}.
     */
    public abstract InetAddress findGateway(BeamRuntime runtime);

    public Map<String, String> findCredentials() {
        return findCredentials(false);
    }

    public abstract Map<String, String> findCredentials(boolean refresh);

    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
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

    public Set<String> getIncludedLayers() {
        if (includedLayers == null) {
            includedLayers = new HashSet<>();
        }

        return includedLayers;
    }

    public void setIncludedLayers(Set<String> includedLayers) {
        this.includedLayers = includedLayers;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
                (other != null &&
                getClass().equals(other.getClass()));
    }

    @Override
    public String toString() {
        return "Cloud: " + getName();
    }

    public abstract String copyDeploymentFile(String storageName, String storageRegion, String buildsKey, String oldBuildsKey, String commonKey, Object pending);

    public abstract void consoleLogin(boolean readonly, boolean urlOnly, PrintWriter out) throws Exception;
}
