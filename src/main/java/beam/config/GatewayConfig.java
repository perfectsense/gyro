package beam.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class GatewayConfig extends Config {

    private String ipAddress;
    private String publicIpAddress;
    private String instanceType;
    private String instanceRole;
    private String image;
    private List<ProvisionerConfig> provisioners;
    private List<String> securityRules;
    private Set<String> rolePolicies;
    private Boolean elasticIp = Boolean.TRUE;
    private List<VolumeConfig> volumes;
    private List<String> hostnames;
    private List<String> privateHostnames;
    private Map<String, Object> userData;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceRole() {
        return instanceRole;
    }

    public void setInstanceRole(String instanceRole) {
        this.instanceRole = instanceRole;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ProvisionerConfig> getProvisioners() {
        if (provisioners == null) {
            provisioners = new ArrayList<>();
        }
        return provisioners;
    }

    public void setProvisioners(List<ProvisionerConfig> provisioners) {
        this.provisioners = provisioners;
    }

    public List<String> getSecurityRules() {
        if (securityRules == null) {
            securityRules = new ArrayList<>();
        }
        return securityRules;
    }

    public void setSecurityRules(List<String> securityRules) {
        this.securityRules = securityRules;
    }

    public Set<String> getRolePolicies() {
        if (rolePolicies == null) {
            rolePolicies = new HashSet<>();
        }
        return rolePolicies;
    }

    public void setRolePolicies(Set<String> rolePolicies) {
        this.rolePolicies = rolePolicies;
    }

    public Boolean getElasticIp() {
        return elasticIp;
    }

    public void setElasticIp(Boolean elasticIp) {
        this.elasticIp = elasticIp;
    }

    public List<VolumeConfig> getVolumes() {
        if (volumes == null) {
            volumes = new ArrayList<>();
        }

        return volumes;
    }

    public void setVolumes(List<VolumeConfig> volumes) {
        this.volumes = volumes;
    }

    public List<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new ArrayList<>();
        }

        return hostnames;
    }

    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public List<String> getPrivateHostnames() {
        if (privateHostnames == null) {
            privateHostnames = new ArrayList<>();
        }

        return privateHostnames;
    }

    public void setPrivateHostnames(List<String> privateHostnames) {
        this.privateHostnames = privateHostnames;
    }

    public Map<String, Object> getUserData() {
        if (userData == null) {
            userData = new HashMap<>();
        }

        return userData;
    }

    public void setUserData(Map<String, Object> userData) {
        this.userData = userData;
    }
}