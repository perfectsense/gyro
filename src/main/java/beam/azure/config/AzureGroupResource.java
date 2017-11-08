package beam.azure.config;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.azure.AzureCloud;
import beam.config.DeploymentConfig;
import beam.diff.NullSet;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiff;
import beam.diff.ResourceDiffProperty;
import com.google.common.io.BaseEncoding;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.OSProfile;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import java.util.*;

public class AzureGroupResource extends AzureResource<Void> {
    private String name;
    private String groupHash;
    private Set<VirtualMachineResource> virtualMachines;
    private Map<String, String> tags;
    private Set<BeamReference> loadBalancers;
    private String image;
    private String instanceType;
    private int size;
    DeploymentConfig deployment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupHash() {
        return groupHash;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public void setGroupHash(String groupHash) {
        this.groupHash = groupHash;
    }

    @ResourceDiffProperty(updatable = true)
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Set<VirtualMachineResource> getVirtualMachines() {
        if (virtualMachines == null) {
            virtualMachines = new HashSet<>();
        }
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VirtualMachineResource> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new CompactMap<>();
        }
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (this.tags != null && tags != null) {
            this.tags.putAll(tags);

        } else {
            this.tags = tags;
        }
    }

    public Set<BeamReference> getLoadBalancers() {
        if (loadBalancers == null) {
            loadBalancers = new NullSet<>();
        }
        return loadBalancers;
    }

    public void setLoadBalancers(Set<BeamReference> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public DeploymentConfig getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentConfig deployment) {
        this.deployment = deployment;
    }

    @Override
    public String awsId() {
        return getName();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getGroupHash());
    }

    public Map<String, String> getMetaData(AzureCloud cloud) {
        String userData = null;
        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            userData = virtualMachineResource.getUserData();
        }

        Map<String, String> metaData = new LinkedHashMap<>();

        if (userData == null) {
            return metaData;
        }

        String userdataJson = new String(BaseEncoding.base64().decode(userData));
        Map<String, String> rawData = (Map<String, String>) ObjectUtils.fromJson(userdataJson);

        metaData.put("image", getImage());
        metaData.put("instanceType", getInstanceType());

        if (!rawData.containsKey("type")) {
            String buildNumber = rawData.get("buildNumber");
            if (buildNumber != null) {
                String jenkinsBucket = rawData.get("jenkinsBucket");
                String jenkinsWarFile = rawData.get("jenkinsWarFile");
                String jenkinsBuildPath = rawData.get("jenkinsBuildPath");

                metaData.put("buildNumber", buildNumber);
                metaData.put("jenkinsBucket", jenkinsBucket);
                metaData.put("jenkinsBuildPath", jenkinsBuildPath);
                metaData.put("jenkinsWarFile", jenkinsWarFile);
            }

        } else {
            try {
                String pluginName = rawData.get("type");
                Class<?> plugin = Class.forName(pluginName);
                DeploymentConfig deployment = (DeploymentConfig)plugin.getConstructor().newInstance();

                for (String key : deployment.getGroupHashKeys()) {
                    String value = rawData.get(key);
                    metaData.put(key, value);
                }

            } catch (Exception error) {
                error.printStackTrace();
            }
        }

        return metaData;
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, Void cloudResource) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();

        Map<String, List<VirtualMachineResource>> vmByLayerName = new HashMap<>();

        try {
            for (VirtualMachine vm : vMOperations.list(getResourceGroup()).getVirtualMachines()) {
                if (!isInclude(filter, vm) || !getName().equals(vm.getTags().get("group"))) {
                    continue;
                }

                String layerName = vm.getTags().get("beam.layer");
                List<VirtualMachineResource> virtualMachines = vmByLayerName.get(layerName);
                if (virtualMachines == null) {
                    virtualMachines = new ArrayList<>();
                    vmByLayerName.put(layerName, virtualMachines);
                }

                VirtualMachineResource virtualMachineResource = new VirtualMachineResource();
                virtualMachineResource.setRegion(getRegion());
                virtualMachineResource.init(cloud, filter, vm);

                getVirtualMachines().add(virtualMachineResource);
                virtualMachines.add(virtualMachineResource);

                setImage(virtualMachineResource.getImage());
                setInstanceType(virtualMachineResource.getInstanceType());
            }
        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load azure group virtual machines! ");
        }

        for (List<VirtualMachineResource> virtualMachines : vmByLayerName.values()) {
            // Sort instances by launch date.
            Collections.sort(virtualMachines, new Comparator<VirtualMachineResource>() {
                @Override
                public int compare(VirtualMachineResource o1, VirtualMachineResource o2) {
                    String[] o1Parts = o1.getName().split("-");
                    String o1DateString = o1Parts[o1Parts.length-1];

                    String[] o2Parts = o2.getName().split("-");
                    String o2DateString = o2Parts[o2Parts.length-1];

                    return o1DateString.compareTo(o2DateString);
                }
            });

            Integer beamLaunchIndex = 0;
            for (VirtualMachineResource virtualMachine : virtualMachines) {
                virtualMachine.setBeamLaunchIndex(beamLaunchIndex++);
            }
        }

        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            setTags(virtualMachineResource.getTags());
            NetworkInterfaceResource networkInterfaceResource = virtualMachineResource.getNetworkInterfaceResources().get(0);

            if (networkInterfaceResource.getLoadBalancerName() != null) {
                getLoadBalancers().add(newReference(LoadBalancerResource.class, networkInterfaceResource.getLoadBalancerName()));
            }
        }

        Map<String, String> metaData = getMetaData(cloud);
        metaData.remove("instanceType");

        StringBuilder groupHashBuilder = new StringBuilder();
        for (String key : metaData.keySet()) {
            String value = metaData.get(key);
            appendHash(groupHashBuilder, key, value);
        }

        String groupHash = StringUtils.hex(StringUtils.md5(groupHashBuilder.toString()));
        setGroupHash(groupHash);
        setSize(getVirtualMachines().size());
    }

    @Override
    public void diffOnCreate(ResourceChange create) throws Exception {
        create.create(getVirtualMachines());
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, Void> current) throws Exception {
        AzureGroupResource currentGroup = (AzureGroupResource) current;
        update.update(currentGroup.getVirtualMachines(), getVirtualMachines());
    }

    @Override
    public void diffOnDelete(ResourceChange delete) throws Exception {
        delete.delete(getVirtualMachines());
    }

    @Override
    public void create(AzureCloud cloud) {
        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            virtualMachineResource.getTags().put("beam.verifying", "UNDEPLOYED");
            virtualMachineResource.getTags().put("beam.prodASName", "");
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, Void> current, Set<String> changedProperties) {
        String loadBalancerName = null;
        for (BeamReference elb : getLoadBalancers()) {
            loadBalancerName = elb.awsId();
        }

        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            NetworkInterfaceResource networkInterfaceResource = virtualMachineResource.getNetworkInterfaceResources().get(0);
            if (loadBalancerName != null) {
                networkInterfaceResource.setLoadBalancerName(loadBalancerName);
            }
        }
    }

    @Override
    public void delete(AzureCloud cloud) {

    }

    public void deleteVerifyingTag(AzureCloud cloud) {
        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            virtualMachineResource.updateTag(cloud, "beam.verifying", null);
            virtualMachineResource.updateTag(cloud, "beam.prodASName", null);
        }
    }

    public void updateVerifyingTag(AzureCloud cloud, String key, String value) {
        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            virtualMachineResource.updateTag(cloud, key, value);
        }
    }

    public void attachVirtualMachine(AzureCloud cloud, String loadBalancerName) {
        for (VirtualMachineResource virtualMachineResource : getVirtualMachines()) {
            NetworkInterfaceResource networkInterfaceResource = virtualMachineResource.getNetworkInterfaceResources().get(0);
            networkInterfaceResource.setLoadBalancerName(loadBalancerName);
            networkInterfaceResource.update(cloud, networkInterfaceResource, new HashSet<>());
        }
    }

    private <T> T appendHash(StringBuilder sb, String name, T value) {
        sb.append(name);
        sb.append('=');
        sb.append(value instanceof BeamReference ? ((BeamReference) value).awsId() : value);
        sb.append('\n');
        return value;
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("azure group ");
        sb.append(getName());
        return sb.toString();
    }

}
