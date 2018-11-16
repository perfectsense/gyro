package beam.core;

import beam.lang.BeamConfig;
import org.antlr.v4.runtime.misc.Array2DHashSet;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public abstract class BeamCredentials extends BeamConfig {

    private BeamState stateBackend;
    private final Set<BeamResource> dependents = new TreeSet<>();

    /**
     * Return the name of this cloud.
     *
     * @return Never {@code null}.
     */
    public abstract String getName();

    public BeamState getStateBackend() {
        if (stateBackend == null) {
            //return new LocalYamlState();
        }

        return stateBackend;
    }

    public void setStateBackend(BeamState stateBackend) {
        this.stateBackend = stateBackend;
    }

    public abstract void saveState(BeamResource resource);

    public abstract void deleteState(BeamResource resource);

    public Set<BeamResource> dependents() {
        return dependents;
    }

    public Map<String, String> findCredentials() {
        return findCredentials(false);
    }

    public abstract Map<String, String> findCredentials(boolean refresh);

    public Map<String, String> findCredentials(boolean refresh, boolean extended) {
        return findCredentials(refresh);
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
        return "Provider: " + getName();
    }

}
