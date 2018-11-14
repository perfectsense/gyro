package beam.core;

import java.util.Map;

public abstract class BeamCloud {

    private BeamState stateBackend;

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
