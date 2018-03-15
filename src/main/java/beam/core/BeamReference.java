package beam.core;

import com.google.common.base.Preconditions;

import java.util.UUID;

/**
 * BeamReference to an AWS resource.
 */
public class BeamReference {

    private final BeamResource parent;
    private final Class<? extends BeamResource<? extends BeamProvider>> resourceClass;
    private final BeamResource resource;
    private final String awsId;
    private final String beamId;

    /**
     * @param parent Can't be {@code null}.
     * @param resource Can't be {@code null}.
     */
    @SuppressWarnings("unchecked")
    public BeamReference(BeamResource<? extends BeamProvider> parent, BeamResource<? extends BeamProvider> resource) {
        Preconditions.checkNotNull(parent, "parent");
        Preconditions.checkNotNull(resource, "resource");

        this.parent = parent;
        this.resourceClass = (Class<? extends BeamResource<? extends BeamProvider>>) ((Class<?>) resource.getClass());
        this.resource = resource;
        this.awsId = null;

        String beamId = resource.getBeamId();

        if (beamId == null) {
            beamId = UUID.randomUUID().toString();
            resource.setBeamId(beamId);
        }

        this.beamId = beamId;
    }

    /**
     * @param parent Can't be {@code null}.
     * @param resourceClass Can't be {@code null}.
     * @param awsId Can't be {@code null}.
     */
    public BeamReference(BeamResource<? extends BeamProvider> parent, Class<? extends BeamResource<? extends BeamProvider>> resourceClass, String awsId) {
        Preconditions.checkNotNull(parent, "parent");
        Preconditions.checkNotNull(resourceClass, "resourceClass");
        Preconditions.checkNotNull(awsId, "awsId");

        this.parent = parent;
        this.resourceClass = resourceClass;
        this.resource = null;
        this.awsId = awsId;
        this.beamId = null;
    }

    public Class<? extends BeamResource<? extends BeamProvider>> getResourceClass() {
        return resourceClass;
    }

    /**
     * Tries to resolve this reference and return an AWS resource.
     *
     * @return May be {@code null}.
     */
    public BeamResource<? extends BeamProvider> resolve() {
        if (resource != null) {
            return resource;
        }

        if (beamId != null) {
            BeamResource<? extends BeamProvider> resource = parent.findById(resourceClass, beamId);

            if (resource != null) {
                return resource;
            }
        }

        if (awsId != null) {
            return parent.findById(resourceClass, awsId);
        }

        return null;
    }

    public String awsId() {
        if (awsId != null) {
            return awsId;

        } else {
            BeamResource<? extends BeamProvider> resource = resolve();

            if (resource != null) {
                return resource.awsId();
            }
        }

        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;

        } else if (other instanceof BeamReference) {
            BeamReference otherRef = (BeamReference) other;
            String awsId = awsId();

            if (awsId != null && awsId.equals(otherRef.awsId())) {
                return true;

            } else {
                return beamId != null && beamId.equals(otherRef.beamId);
            }

        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String awsId = awsId();

        if (awsId != null) {
            sb.append(awsId);

        } else {
            sb.append("beam:");
            sb.append(beamId);
        }

        return sb.toString();
    }
}
