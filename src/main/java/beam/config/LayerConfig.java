package beam.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class LayerConfig extends Config {

    private String name;
    private String instanceType;
    private String instanceRole;
    private String image;
    private boolean bootstrap;
    private List<ProvisionerConfig> provisioners;
    private List<String> securityRules;
    private DeploymentConfig deployment;
    private List<PlacementConfig> placements;
    private Set<String> rolePolicies;
    private List<VolumeConfig> volumes;
    private Map<String, Object> userData;

    @Deprecated
    private List<Map<String, Object>> services;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
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

    public DeploymentConfig getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentConfig deployment) {
        this.deployment = deployment;
    }

    public List<PlacementConfig> getPlacements() {
        if (placements == null) {
            placements = new ArrayList<>();
        }

        return placements;
    }

    public void setPlacements(List<PlacementConfig> placements) {
        this.placements = placements;
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

    @Deprecated
    public List<Map<String, Object>> getServices() {
        if (services == null) {
            services = new ArrayList<>();
        }
        return services;
    }

    @Deprecated
    public void setServices(List<Map<String, Object>> services) {
        this.services = services;
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