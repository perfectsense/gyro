package beam.core;

import beam.Beam;
import beam.core.diff.DiffUtil;
import com.google.common.base.Preconditions;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BeamReference to an AWS resource.
 */
public class BeamReference {

    private String key;
    private static Pattern REFERENCE_PATTERN = Pattern.compile("[$][{](?<resource>[a-zA-Z][a-zA-Z_0-9]*)(?<properties>([.]([a-zA-Z][a-zA-Z_0-9]*))*)[}]");
    private final BeamResource parent;
    private final Class<? extends BeamResource<? extends BeamProvider>> resourceClass;
    private BeamResource resource;
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

    public BeamReference(Map<String, BeamResource> symbolTable, String key) {
        this.parent = null;
        this.resourceClass = null;
        this.resource = null;
        this.awsId = null;
        this.beamId = null;
        this.key = key;
        Matcher matcher = REFERENCE_PATTERN.matcher(key);

        if (matcher.find()) {
            String resourceName = matcher.group("resource");
            resource = symbolTable.get(resourceName);
            if (resource == null) {
                throw new BeamException(String.format("Reference %s is not defined for %s", resourceName, key));
            }
        }
    }

    public BeamReference(String key) {
        this.parent = null;
        this.resourceClass = null;
        this.resource = null;
        this.awsId = null;
        this.beamId = null;
        this.key = key;
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
            sb.append(getKey());
        }

        return sb.toString();
    }

    public Object resolveReference() {
        try {
            Matcher matcher = REFERENCE_PATTERN.matcher(key);

            if (matcher.find()) {
                Object reference = resource;
                String properties = matcher.group("properties");
                if (properties != null) {
                    properties = properties.substring(1);
                    List<String> readerNames = Arrays.asList(properties.split("\\."));
                    for (String readerName : readerNames) {
                        if (reference != null) {
                            reference = DiffUtil.getPropertyValue(reference, null, readerName);
                        }
                    }
                }

                return reference;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isReference(String key) {
        Matcher matcher = REFERENCE_PATTERN.matcher(key);
        return matcher.find();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public BeamResource getResource() {
        return resource;
    }

    public void setResource(BeamResource resource) {
        this.resource = resource;
    }
}
