package beam.handlers;

import beam.BeamInstance;
import com.psddev.dari.util.ObjectUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ServiceInstance extends BeamInstance {

    private String id;
    private String environment;
    private String location;
    private String layer;
    private Set<Service> services;
    private String privateIp;
    private String publicIp;
    private Long launchTime;
    private Long lastPing;
    private Mark mark = Mark.AVAILABLE;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getRegion() {
        if (!ObjectUtils.isBlank(getLocation())) {
            return getLocation().split(":")[0];
        }

        return null;
    }

    @Override
    public boolean isSandboxed() {
        return false;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isInLocation(String otherLocation) {
        return getLocation() != null && getLocation().equals(otherLocation);
    }

    public boolean isNearLocation(String otherLocation) {
        if (otherLocation == null || otherLocation.isEmpty()) {
            return false;
        }

        if (getLocation() == null || getLocation().isEmpty()) {
            return false;
        }

        String locationPart = getLocation().split(":")[0];
        String otherLocationPart = otherLocation.split(":")[0];

        return locationPart.equals(otherLocationPart);
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public String getLayer() {
        return layer;
    }

    @Override
    public String getState() {
        return "Unknown";
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    @Override
    public Set<String> getServices() {
        Set<String> serviceNames = new HashSet<>();

        for (Service service : getServiceInfo()) {
            serviceNames.add(service.getName());
        }

        return serviceNames;
    }

    public Set<Service> getServiceInfo() {
        if (services == null) {
            services = new HashSet<>();
        }

        return services;
    }

    public Service getServiceInfo(String serviceName) {
        for (Service service : getServiceInfo()) {
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }

        return null;
    }

    public void setServiceInfo(Set<Service> services) {
        this.services = services;
    }

    public boolean doesProvideService(String serviceName) {
        return getServiceInfo(serviceName) != null;
    }

    public void addServiceInfo(Service service) {
        getServiceInfo().add(service);
    }

    @Override
    public String getPublicIpAddress() {
        return publicIp;
    }

    public void setPublicIpAddress(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIpAddress() {
        return privateIp;
    }

    public void setPrivateIpAddress(String privateIp) {
        this.privateIp = privateIp;
    }

    @Override
    public Date getDate() {
        return new Date();
    }

    public Long getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(Long launchTime) {
        this.launchTime = launchTime;
    }

    public Long getLastPing() {
        return lastPing;
    }

    public void setLastPing(Long lastPing) {
        this.lastPing = lastPing;
    }

    public Mark getMark() {
        if (mark == null) {
            this.mark = Mark.AVAILABLE;
        }

        return mark;
    }

    public void setMark(Mark mark) {
        if (mark == null) {
            this.mark = Mark.AVAILABLE;

        } else {
            this.mark = mark;
        }
    }

    public boolean isOk() {
        return lastPing > System.currentTimeMillis() - 60000 && getMark() == Mark.AVAILABLE;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %s]",
                getId(), getLayer(), getPrivateIpAddress());
    }

    public static enum Mark {
        AVAILABLE("Available"),
        UNAVAILABLE("Unavailable");

        private String name;
        private long timeStamp;

        Mark(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public long getTimeStamp() {
            return this.timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }

    public static class Service {

        private String name;
        private Set<String> primaries;
        private Integer status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<String> getPrimaries() {
            if (primaries == null) {
                primaries = new HashSet<>();
            }

            return primaries;
        }

        public void setPrimaries(Set<String> primaries) {
            this.primaries = primaries;
        }

        public void addPrimary(String primary) {
            getPrimaries().add(primary);
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public boolean isOk() {
            return status == 0;
        }

    }

}