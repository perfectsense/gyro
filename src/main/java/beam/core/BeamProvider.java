package beam.core;

import java.util.Map;

public abstract class BeamProvider extends BeamObject {

    /**
     * Return the name of this provider.
     *
     * @return Never {@code null}.
     */
    public abstract String getName();

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
