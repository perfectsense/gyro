package beam.azure.config;

import beam.*;
import beam.azure.AzureCloud;
import beam.diff.ResourceChange;
import beam.diff.ResourceDiffProperty;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.VirtualMachineOperations;
import com.microsoft.azure.management.compute.models.*;
import com.microsoft.azure.management.network.NetworkInterfaceOperations;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.models.NetworkInterface;
import com.psddev.dari.util.CompactMap;
import java.util.*;

public class VirtualMachineResource extends AzureResource<VirtualMachine> {

    private String id;
    private String name;
    private String image;
    private String instanceType;
    private String privateIpAddress;
    private String publicIpAddress;
    private String state;
    private BeamReference subnet;
    private String userData;
    private Integer beamLaunchIndex;
    private List<NetworkInterfaceResource> networkInterfaceResources;
    private String availabilitySet;
    private BeamReference keyPair;
    private Set<DiskResource> diskResources;
    private boolean updating;

    private final String userName = "ubuntu";
    private final String publicKeyPath = "/home/" + userName + "/.ssh/authorized_keys";

    private Map<String, String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(String availabilitySet) {
        this.availabilitySet = availabilitySet;
    }

    @ResourceDiffProperty
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @ResourceDiffProperty(updatable = true)
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

    public List<NetworkInterfaceResource> getNetworkInterfaceResources() {
        if (networkInterfaceResources == null) {
            networkInterfaceResources = new ArrayList<>();
        }
        return networkInterfaceResources;
    }

    public void setNetworkInterfaceResources(List<NetworkInterfaceResource> networkInterfaceResources) {
        this.networkInterfaceResources = networkInterfaceResources;
    }

    public Set<DiskResource> getDiskResources() {
        if (diskResources == null) {
            diskResources = new HashSet<>();
        }
        return diskResources;
    }

    public void setDiskResources(Set<DiskResource> diskResources) {
        this.diskResources = diskResources;
    }

    @ResourceDiffProperty(updatable = true)
    public int getAdditionalDisk() {
        return getDiskResources().size();
    }

    @Override
    public String awsId() {
        return getId();
    }

    @ResourceDiffProperty(updatable = true)
    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @ResourceDiffProperty
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    @ResourceDiffProperty(updatable = true)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public BeamReference getSubnet() {
        return newParentReference(SubnetResource.class, subnet);
    }

    public void setSubnet(BeamReference subnet) {
        this.subnet = subnet;
    }

    public BeamReference getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(BeamReference keyPair) {
        this.keyPair = keyPair;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public Integer getBeamLaunchIndex() {
        return beamLaunchIndex;
    }

    public void setBeamLaunchIndex(Integer beamLaunchIndex) {
        this.beamLaunchIndex = beamLaunchIndex;
    }

    private boolean isUpdating() {
        return updating;
    }

    private void setUpdating(boolean updating) {
        this.updating = updating;
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getTags().get("beam.layer") + " " + getBeamLaunchIndex());
    }

    @Override
    public void init(AzureCloud cloud, BeamResourceFilter filter, VirtualMachine vm) {
        setName(vm.getName());
        setTags(vm.getTags());
        setId(vm.getId());
        setInstanceType(vm.getHardwareProfile().getVirtualMachineSize());

        OSProfile osProfile = vm.getOSProfile();
        setUserData(osProfile.getCustomData());

        StorageProfile storageProfile = vm.getStorageProfile();

        for (DataDisk dataDisk : storageProfile.getDataDisks()) {
            DiskResource diskResource = new DiskResource();
            diskResource.setRegion(getRegion());
            diskResource.setVirtualMachine(diskResource.newReference(this));
            diskResource.init(cloud, filter, dataDisk);
            getDiskResources().add(diskResource);
        }

        OSDisk osDisk = storageProfile.getOSDisk();
        VirtualHardDisk disk = osDisk.getSourceImage();
        String imageUri = disk.getUri();
        String[] parts = imageUri.split("/");
        String imageName = parts[parts.length-1];

        imageName = imageName.substring(0, imageName.length()-".vhd".length());
        setImage(imageName);

        if (vm.getAvailabilitySetReference() != null) {
            setAvailabilitySet(vm.getAvailabilitySetReference().getReferenceUri());
        }

        ComputeManagementClient computeManagementClient = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = computeManagementClient.getVirtualMachinesOperations();

        try {
            VirtualMachine virtualMachine = vMOperations.getWithInstanceView(getResourceGroup(), getName()).getVirtualMachine();
            for (InstanceViewStatus status : virtualMachine.getInstanceView().getStatuses()) {
                if (status.getDisplayStatus().startsWith("VM")) {
                    setState(status.getDisplayStatus());
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to load virtual machine with instance view!");
        }

        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkInterfaceOperations nIOperations = client.getNetworkInterfacesOperations();

        for (NetworkInterfaceReference nicReference : vm.getNetworkProfile().getNetworkInterfaces()) {
            String nicUri = nicReference.getReferenceUri();
            String[] uriParts = nicUri.split("/");
            String nicName = uriParts[uriParts.length-1];

            try {
                NetworkInterface nic = nIOperations.get(getResourceGroup(), nicName).getNetworkInterface();
                NetworkInterfaceResource networkInterfaceResource = new NetworkInterfaceResource();
                networkInterfaceResource.setRegion(getRegion());
                networkInterfaceResource.setVirtualMachine(networkInterfaceResource.newReference(this));
                networkInterfaceResource.init(cloud, filter, nic);
                getNetworkInterfaceResources().add(networkInterfaceResource);

                setPrivateIpAddress(networkInterfaceResource.getPrivateIp());
                setPublicIpAddress(networkInterfaceResource.getPublicIp());

            } catch (Exception error) {
                error.printStackTrace();
                throw new BeamException("Fail to load network interface: " + getName());
            }
        }
    }

    @Override
    public void diffOnUpdate(ResourceChange update, BeamResource<AzureCloud, VirtualMachine> current) throws Exception {
        VirtualMachineResource currentVM = (VirtualMachineResource) current;
        for (NetworkInterfaceResource networkInterfaceResource : getNetworkInterfaceResources()) {
            networkInterfaceResource.setName(currentVM.getName());
        }

        update.update(currentVM.getNetworkInterfaceResources(), getNetworkInterfaceResources());
        update.update(currentVM.getDiskResources(), getDiskResources());
    }

    @Override
    public void create(AzureCloud cloud) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();
        VirtualMachine vm = new VirtualMachine();
        vm.setName(getName());
        vm.setLocation(getRegion());

        if (getAvailabilitySet() != null) {
            AvailabilitySetReference availabilitySetReference = new AvailabilitySetReference();
            availabilitySetReference.setReferenceUri(getAvailabilitySet());
            vm.setAvailabilitySetReference(availabilitySetReference);
        }

        NetworkProfile networkProfile = new NetworkProfile();
        ArrayList<NetworkInterfaceReference> networkInterfaceReferences = new ArrayList<>();
        networkProfile.setNetworkInterfaces(networkInterfaceReferences);

        for (NetworkInterfaceResource networkInterfaceResource : getNetworkInterfaceResources()) {
            SubnetResource subnetResource = (SubnetResource)getSubnet().resolve();

            networkInterfaceResource.setSubnetId(subnetResource.getId());
            networkInterfaceResource.create(cloud);

            NetworkInterfaceReference networkInterfaceReference = new NetworkInterfaceReference();
            networkInterfaceReference.setReferenceUri(networkInterfaceResource.getId());
            networkInterfaceReferences.add(networkInterfaceReference);
        }

        vm.setNetworkProfile(networkProfile);

        HashMap<String, String> tags = new HashMap<>();
        tags.putAll(getTags());
        vm.setTags(tags);

        HardwareProfile hdProfile = new HardwareProfile();
        hdProfile.setVirtualMachineSize(getInstanceType());
        vm.setHardwareProfile(hdProfile);

        StorageProfile storageProfile = new StorageProfile();

        for (DiskResource diskResource : getDiskResources()) {
            DataDisk dataDisk = new DataDisk();
            dataDisk.setName(diskResource.getName());
            dataDisk.setCaching("None");

            VirtualHardDisk virtualHardDisk = new VirtualHardDisk();
            virtualHardDisk.setUri(cloud.getStorageAccountBase() + diskResource.getName() + ".vhd");
            dataDisk.setVirtualHardDisk(virtualHardDisk);

            dataDisk.setLun(diskResource.getLun());
            dataDisk.setCreateOption("Empty");
            dataDisk.setDiskSizeGB(diskResource.getSize());

            if (storageProfile.getDataDisks() == null) {
                storageProfile.setDataDisks(new ArrayList<>());
            }

            storageProfile.getDataDisks().add(dataDisk);
        }

        String name = getName();
        String createOption = "FromImage";
        VirtualHardDisk targetDisk = new VirtualHardDisk();
        targetDisk.setUri(cloud.getStorageAccountBase() + getName() + ".vhd");

        OSDisk osDisk = new OSDisk(name, targetDisk, createOption);
        osDisk.setCaching("ReadWrite");
        osDisk.setOperatingSystemType("Linux");

        VirtualHardDisk disk = new VirtualHardDisk();
        disk.setUri(cloud.getStorageImagePath() + getImage() + ".vhd");
        osDisk.setSourceImage(disk);

        storageProfile.setOSDisk(osDisk);
        vm.setStorageProfile(storageProfile);

        OSProfile osProfile = new OSProfile();
        osProfile.setComputerName(getName());
        osProfile.setCustomData(getUserData());
        osProfile.setAdminUsername(userName);

        LinuxConfiguration configuration = new LinuxConfiguration();
        SshConfiguration sshConfiguration = new SshConfiguration();

        ArrayList<SshPublicKey> keys = new ArrayList<>();
        SshPublicKey key = new SshPublicKey();

        KeyPairResource keyPairResource = (KeyPairResource)getKeyPair().resolve();
        key.setKeyData(keyPairResource.getPublicKey());
        key.setPath(publicKeyPath);
        keys.add(key);

        sshConfiguration.setPublicKeys(keys);
        configuration.setSshConfiguration(sshConfiguration);
        osProfile.setLinuxConfiguration(configuration);

        vm.setOSProfile(osProfile);

        try {
            ComputeLongRunningOperationResponse response = vMOperations.createOrUpdate(getResourceGroup(), vm);
            if (!"Succeeded".equals(response.getStatus().toString())) {
                throw new BeamException(response.getError().getMessage());
            }

            vMOperations.beginStarting(getResourceGroup(), getName());

        } catch (Exception error) {
            if (!isUpdating()) {
                delete(cloud);
            }

            error.printStackTrace();
            throw new BeamException("Fail to create or update virtual machine: " + getName());
        }
    }

    @Override
    public void update(AzureCloud cloud, BeamResource<AzureCloud, VirtualMachine> current, Set<String> changedProperties) {
        setUpdating(true);
        VirtualMachineResource currentVM = (VirtualMachineResource) current;
        setName(currentVM.getName());
        setUserData(((VirtualMachineResource) current).getUserData());
        create(cloud);
    }

    public void updateTag(AzureCloud cloud, String key, String value) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();

        try {
            for (VirtualMachine vm : vMOperations.list(getResourceGroup()).getVirtualMachines()) {
                if (vm.getName().equals(getName())) {
                    if (value != null) {
                        vm.getTags().put(key, value);
                    } else {
                        vm.getTags().remove(key);
                    }

                    vMOperations.createOrUpdate(getResourceGroup(), vm);
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to update virtual machine tag!");
        }
    }

    @Override
    public void delete(AzureCloud cloud) {
        ComputeManagementClient client = cloud.createComputeManagementClient();
        VirtualMachineOperations vMOperations = client.getVirtualMachinesOperations();
        try {
            vMOperations.delete(getResourceGroup(), getName());

        } catch (Exception error) {
            error.printStackTrace();
            throw new BeamException("Fail to delete virtual machine: " + getName());
        }

        for (NetworkInterfaceResource networkInterfaceResource : getNetworkInterfaceResources()) {
            networkInterfaceResource.delete(cloud);
        }

        for (DiskResource diskResource : getDiskResources()) {
            diskResource.delete(cloud);
        }

        DiskResource osDisk = new DiskResource();
        osDisk.setRegion(getRegion());
        osDisk.setName(getName());
        osDisk.delete(cloud);
    }

    public String getIdFromName(AzureCloud cloud) {
        return String.format("%s%s%s%s%s%s%s%s", "/subscriptions/", cloud.getCredentials().getSubscription(),
                "/resourceGroups/", getResourceGroup(),
                "/providers/", "Microsoft.Compute",
                "/virtualMachines/", getName());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(' ');
        sb.append(BeamRuntime.getCurrentRuntime().getProject());
        sb.append(' ');
        sb.append(getTags().get("beam.layer"));

        return sb.toString();
    }
}
