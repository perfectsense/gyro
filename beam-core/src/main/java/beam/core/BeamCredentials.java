package beam.core;

import beam.lang.BeamLanguageExtension;

import java.util.Map;

public abstract class BeamCredentials extends BeamLanguageExtension implements Comparable<BeamCredentials> {

    private BeamState stateBackend;

    /**
     * Return the name of this cloud.
     *
     * @return Never {@code null}.
     */
    public abstract String getCloudName();

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
        return this == other || (other != null && getClass().equals(other.getClass()));
    }

    @Override
    public int compareTo(BeamCredentials o) {
        if (o == null) {
            return 1;
        }

        String compareKey = String.format("%s %s", getResourceType(), getResourceIdentifier());
        String otherKey = String.format("%s %s", o.getResourceType(), o.getResourceIdentifier());

        return compareKey.compareTo(otherKey);
    }

}
