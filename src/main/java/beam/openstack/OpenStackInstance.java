package beam.openstack;

import beam.BeamInstance;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;

public class OpenStackInstance extends BeamInstance {

    private Server server;
    private String region;
    private OpenStackCloud osCloud;
    private Map<String, Object> instanceMap;

    public OpenStackInstance(OpenStackCloud osCloud, Server server, String region) {
        this.osCloud = osCloud;
        this.server = server;
        this.region = region;
    }

    public OpenStackInstance(OpenStackCloud cloud, Map<String, Object> instanceMap) {
        this.osCloud = cloud;
        this.server = null;
        this.instanceMap = instanceMap;
    }

    public Map<String, Object> getInstanceMap() {
        return instanceMap;
    }

    public void setInstanceMap(Map<String, Object> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public String getId() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("serverId"));
        }

        return "rs-" + server.getId().split("-")[0];
    }

    @Override
    public String getEnvironment() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("environment"));
        }

        return server.getMetadata().get("environment");
    }

    @Override
    public String getLocation() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("location"));
        }

        return region;
    }

    @Override
    public String getRegion() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("region"));
        }

        return region;
    }

    @Override
    public String getLayer() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("layer"));
        }

        return server.getMetadata().get("layer");
    }

    @Override
    public String getState() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("serverState"));
        }

        return server.getStatus().name();
    }

    @Override
    public boolean isSandboxed() {
        if (instanceMap != null) {
            return ObjectUtils.to(Boolean.class, instanceMap.get("sandbox"));
        }

        return Boolean.TRUE.equals(server.getMetadata().get("sandbox"));
    }

    @Override
    public String getPublicIpAddress() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("publicIp"));
        }

        for (String networkName : server.getAddresses().keySet()) {
            Collection<Address> addresses = server.getAddresses().get(networkName);

            if (networkName.startsWith("public")) {
                Iterator<Address> iter = addresses.iterator();
                while (iter.hasNext()) {
                    Address address = iter.next();
                    if (address.getVersion() == 4) {
                        return address.getAddr();
                    }
                }
            }
        }

        return server.getAccessIPv4();
    }

    @Override
    public String getPrivateIpAddress() {
        if (instanceMap != null) {
            return ObjectUtils.to(String.class, instanceMap.get("privateIp"));
        }

        String project = server.getMetadata().get("project");

        for (String networkName : server.getAddresses().keySet()) {
            Collection<Address> addresses = server.getAddresses().get(networkName);

            if (networkName.startsWith(project)) {
                Iterator<Address> iter = addresses.iterator();
                while (iter.hasNext()) {
                    Address address = iter.next();
                    if (address.getVersion() == 4) {
                        return address.getAddr();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Date getDate() {
        if (instanceMap != null) {
            return new Date(ObjectUtils.to(Long.class, instanceMap.get("date")));
        }

        return server.getCreated();
    }
}