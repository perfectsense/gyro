package beam;

import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import com.google.common.collect.ComparisonChain;

public abstract class BeamInstance implements Comparable<BeamInstance> {

    public abstract String getId();

    public abstract String getEnvironment();

    public abstract String getLocation();

    public abstract String getRegion();

    public abstract String getLayer();

    public abstract String getState();

    public abstract boolean isSandboxed();

    public Set<String> getServices() {
        return new HashSet<>();
    }

    public abstract String getPublicIpAddress();

    public abstract String getPrivateIpAddress();

    public abstract Date getDate();

    public String getHostname() {
        return getId() + "." +
                getLayer() + ".layer." +
                getEnvironment() + "." +
                BeamRuntime.getCurrentRuntime().getInternalDomain();
    }

    @Override
    public int compareTo(BeamInstance other) {
        return ComparisonChain.start()
                .compare(getLocation(), other.getLocation())
                .compare(getLayer(), other.getLayer())
                .compare(getDate(), other.getDate())
                .result();
    }
}
