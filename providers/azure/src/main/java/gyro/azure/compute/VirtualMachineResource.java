package gyro.azure.compute;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.CachingTypes;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithManagedCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithNetwork;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPrivateIP;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPublicIPAddress;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsCreateManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsCreateUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;
import gyro.azure.AzureResource;
import gyro.core.BeamException;
import gyro.core.diff.ResourceDiffProperty;
import gyro.core.diff.ResourceName;
import gyro.core.diff.ResourceOutput;
import gyro.lang.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a virtual machine.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     azure::virtual-machine virtual-machine-example
 *          virtual-machine-name: "virtual-machine-example"
 *          resource-group-name: $(azure::resource-group resource-group-example-VM | resource-group-name)
 *          network-id: $(azure::network network-example-VM | network-id)
 *          subnet: "subnet1"
 *          os-type: "linux"
 *          disk-id: $(azure::disk disk-example-VM| disk-id)
 *          network-interface-name: $(azure::network-interface network-interface-example-VM | network-interface-name)
 *          vm-image-type: "popular"
 *          known-virtual-image: "UBUNTU_SERVER_14_04_LTS"
 *          admin-user-name: "qwerty@123"
 *          admin-password: "qwerty@123"
 *          caching-type: "NONE"
 *          vm-size-type: "STANDARD_G1"
 *          storage-account-type-data-disk: "STANDARD_LRS"
 *          storage-account-type-os-disk: "STANDARD_LRS"
 *
 *          tags: {
 *              Name: "virtual-machine-example"
 *          }
 *     end
 */
@ResourceName("virtual-machine")
public class VirtualMachineResource extends AzureResource {
    private String virtualMachineName;
    private String resourceGroupName;
    private String networkId;
    private String networkInterfaceName;
    private String adminUserName;
    private String adminPassword;
    private String virtualMachineId;
    private String vmId;
    private String publicIpAddressName;
    private String privateIpAddress;
    private String osType;
    private String diskId;
    private String subnet;
    private String vmImageType;
    private String ssh;
    private String storedImage;
    private String customImage;
    private String galleryImageVersion;
    private String cachingType;
    private String storageAccountTypeDataDisk;
    private String storageAccountTypeOsDisk;
    private String vmSizeType;
    private String knownVirtualImage;
    private String timeZone;
    private String imagePublisher;
    private String imageOffer;
    private String imageSku;
    private String imageRegion;
    private String imageVersion;
    private List<String> secondaryNetworkInterfaceNames;
    private Map<String, String> tags;

    /**
     * Name of the virtual machine. (Required)
     */
    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    /**
     * Name of the resource group under which this would reside. (Required)
     */
    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    /**
     * Id of the virtual network which would be associated with this. (Required)
     */
    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * Name of the network interface that the virtual machine would use.
     */
    public String getNetworkInterfaceName() {
        return networkInterfaceName;
    }

    public void setNetworkInterfaceName(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
    }

    /**
     * Login user name for the virtual machine.
     */
    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    /**
     * Login password for the virtual machine.
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @ResourceOutput
    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    @ResourceOutput
    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    /**
     * The name of the public ip address associated with the virtual machine.
     */
    public String getPublicIpAddressName() {
        return publicIpAddressName;
    }

    public void setPublicIpAddressName(String publicIpAddressName) {
        this.publicIpAddressName = publicIpAddressName;
    }

    /**
     * Private ip address associated with the virtual machine.
     */
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    /**
     * The os for the virtual machine. Valid options ['Linux','Unix']. (Required)
     */
    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    /**
     * The id of a disk to be attached to the virtual machine.
     */
    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    /**
     * One of the subnet name from the assigned virtual network.
     */
    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    /**
     * Type of virtual machine image. Defaults to specialized. Valid options ['popular', 'specialized', 'latest', 'specific', 'custom', 'gallery'] (Required)
     */
    public String getVmImageType() {
        if (vmImageType == null) {
            vmImageType = "specialized";
        }

        return vmImageType;
    }

    public void setVmImageType(String vmImageType) {
        this.vmImageType = vmImageType;
    }

    /**
     * The ssh to be used to log in to the virtual machine.
     */
    public String getSsh() {
        return ssh;
    }

    public void setSsh(String ssh) {
        this.ssh = ssh;
    }

    /**
     * The id of a stored image to create the virtual machine.
     */
    public String getStoredImage() {
        return storedImage;
    }

    public void setStoredImage(String storedImage) {
        this.storedImage = storedImage;
    }

    /**
     * The id of a custom image to create the virtual machine.
     */
    public String getCustomImage() {
        return customImage;
    }

    public void setCustomImage(String customImage) {
        this.customImage = customImage;
    }

    /**
     * The version of a gallery image to create the virtual machine.
     */
    public String getGalleryImageVersion() {
        return galleryImageVersion;
    }

    public void setGalleryImageVersion(String galleryImageVersion) {
        this.galleryImageVersion = galleryImageVersion;
    }

    /**
     * The caching type for the virtual machine.
     */
    public String getCachingType() {
        return cachingType;
    }

    public void setCachingType(String cachingType) {
        this.cachingType = cachingType;
    }

    /**
     * The data disk storage account type for the virtual machine. Valid options include [ 'STANDARD_LRS', 'PREMIUM_LRS', 'STANDARD_SSD_LRS'].
     */
    public String getStorageAccountTypeDataDisk() {
        return storageAccountTypeDataDisk;
    }

    public void setStorageAccountTypeDataDisk(String storageAccountTypeDataDisk) {
        this.storageAccountTypeDataDisk = storageAccountTypeDataDisk;
    }

    /**
     * The os disk storage account type for the virtual machine. Valid options include [ 'STANDARD_LRS', 'PREMIUM_LRS', 'STANDARD_SSD_LRS'].
     */
    public String getStorageAccountTypeOsDisk() {
        return storageAccountTypeOsDisk;
    }

    public void setStorageAccountTypeOsDisk(String storageAccountTypeOsDisk) {
        this.storageAccountTypeOsDisk = storageAccountTypeOsDisk;
    }

    /**
     * The size of the virtual machine.
     */
    public String getVmSizeType() {
        return vmSizeType;
    }

    public void setVmSizeType(String vmSizeType) {
        this.vmSizeType = vmSizeType;
    }

    /**
     * The known virtual machine image type.
     */
    public String getKnownVirtualImage() {
        return knownVirtualImage;
    }

    public void setKnownVirtualImage(String knownVirtualImage) {
        this.knownVirtualImage = knownVirtualImage;
    }

    /**
     * The time zone for the virtual machine.
     */
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * The publisher of the image to be used for creating the virtual image. Needed if 'vm-image-type' selected as 'latest' or 'specific'
     */
    public String getImagePublisher() {
        return imagePublisher;
    }

    public void setImagePublisher(String imagePublisher) {
        this.imagePublisher = imagePublisher;
    }

    /**
     * The offer of the image to be used for creating the virtual image. Needed if 'vm-image-type' selected as 'latest' or 'specific'
     */
    public String getImageOffer() {
        return imageOffer;
    }

    public void setImageOffer(String imageOffer) {
        this.imageOffer = imageOffer;
    }

    /**
     * The SKU of the image to be used for creating the virtual image. Needed if 'vm-image-type' selected as 'latest' or 'specific'
     */
    public String getImageSku() {
        return imageSku;
    }

    public void setImageSku(String imageSku) {
        this.imageSku = imageSku;
    }

    /**
     * The region where the image resides to be used for creating the virtual image. Needed if 'vm-image-type' selected as 'specific'
     */
    public String getImageRegion() {
        return imageRegion;
    }

    public void setImageRegion(String imageRegion) {
        this.imageRegion = imageRegion;
    }

    /**
     * The version of the image to be used for creating the virtual image. Needed if 'vm-image-type' selected as 'specific'
     */
    public String getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(String imageVersion) {
        this.imageVersion = imageVersion;
    }

    /**
     * A list of secondary network interface that the virtual machine would use.
     */
    public List<String> getSecondaryNetworkInterfaceNames() {
        if (secondaryNetworkInterfaceNames == null) {
            secondaryNetworkInterfaceNames = new ArrayList<>();
        }

        return secondaryNetworkInterfaceNames;
    }

    public void setSecondaryNetworkInterfaceNames(List<String> secondaryNetworkInterfaceNames) {
        this.secondaryNetworkInterfaceNames = secondaryNetworkInterfaceNames;
    }

    @ResourceDiffProperty(updatable = true)
    public Map<String, String> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean refresh() {
        Azure client = createClient();

        VirtualMachine virtualMachine = client.virtualMachines().getById(getVirtualMachineId());

        setVirtualMachineName(virtualMachine.name());
        setVmId(virtualMachine.vmId());
        setTags(virtualMachine.tags());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        WithNetwork withNetwork = client.virtualMachines().define(getVirtualMachineName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName());

        WithOS withOS;

        if (!ObjectUtils.isBlank(getNetworkInterfaceName())) {
            withOS = withNetwork.withExistingPrimaryNetworkInterface(
                    client.networkInterfaces().getByResourceGroup(
                        getResourceGroupName(), getNetworkInterfaceName()
                    ));
        } else {

            WithPrivateIP withPrivateIP = withNetwork
                .withExistingPrimaryNetwork(client.networks().getById(getNetworkId()))
                .withSubnet(getSubnet());

            WithPublicIPAddress withPublicIpAddress;
            if (!ObjectUtils.isBlank(getPrivateIpAddress())) {
                withPublicIpAddress = withPrivateIP.withPrimaryPrivateIPAddressStatic(getPrivateIpAddress());
            } else {
                withPublicIpAddress = withPrivateIP.withPrimaryPrivateIPAddressDynamic();
            }


            if (!ObjectUtils.isBlank(getPublicIpAddressName())) {
                withOS = withPublicIpAddress.withExistingPrimaryPublicIPAddress(
                    client.publicIPAddresses().getByResourceGroup(getResourceGroupName(), getPublicIpAddressName())
                );
            } else {
                withOS = withPublicIpAddress.withoutPrimaryPublicIPAddress();
            }
        }



        WithCreate create = null;
        WithManagedCreate managedCreate = null;

        boolean isLatestPopularOrSpecific = getVmImageType().equals("latest")
            || getVmImageType().equals("popular")
            || getVmImageType().equals("specific");

        if (getOsType().equals("linux")) {
            //linux

            WithLinuxCreateUnmanaged createUnmanaged = null;
            WithLinuxCreateManaged createManaged = null;

            if (isLatestPopularOrSpecific) {
                WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged managedOrUnmanaged;
                WithLinuxCreateManagedOrUnmanaged createManagedOrUnmanaged;

                if (getVmImageType().equals("latest")) {
                    managedOrUnmanaged = withOS.withLatestLinuxImage(getImagePublisher(),getImageOffer(),getImageSku())
                        .withRootUsername(getAdminUserName());
                } else if (getVmImageType().equals("popular")) {
                    managedOrUnmanaged = withOS.withPopularLinuxImage(
                        KnownLinuxVirtualMachineImage.valueOf(getKnownVirtualImage())
                    ).withRootUsername(getAdminUserName());
                } else {
                    managedOrUnmanaged = withOS.withSpecificLinuxImageVersion(
                        client.virtualMachineImages()
                            .getImage(getImageRegion(),getImagePublisher(),getImageOffer(),getImageSku(),getImageVersion())
                            .imageReference()
                    ).withRootUsername(getAdminUserName());
                }

                if (!ObjectUtils.isBlank(getAdminPassword()) && !ObjectUtils.isBlank(getSsh())) {
                    managedCreate = managedOrUnmanaged.withRootPassword(getAdminPassword()).withSsh(getSsh());
                } else if (!ObjectUtils.isBlank(getAdminPassword())) {
                    managedCreate = managedOrUnmanaged.withRootPassword(getAdminPassword());
                } else {
                    managedCreate = managedOrUnmanaged.withSsh(getSsh());
                }

            } else if (getVmImageType().equals("stored")) {
                WithLinuxRootPasswordOrPublicKeyUnmanaged publicKeyUnmanaged = withOS
                    .withStoredLinuxImage(getStoredImage())
                    .withRootUsername(getAdminUserName());

                if (!ObjectUtils.isBlank(getAdminPassword()) && !ObjectUtils.isBlank(getSsh())) {
                    createUnmanaged = publicKeyUnmanaged.withRootPassword(getAdminPassword()).withSsh(getSsh());
                } else if (!ObjectUtils.isBlank(getAdminPassword())) {
                    createUnmanaged = publicKeyUnmanaged.withRootPassword(getAdminPassword());
                } else {
                    createUnmanaged = publicKeyUnmanaged.withSsh(getSsh());
                }

            } else if (getVmImageType().equals("custom") || getVmImageType().equals("gallery")) {
                WithLinuxRootPasswordOrPublicKeyManaged publicKeyManaged;

                if (getVmImageType().equals("custom")) {
                    publicKeyManaged = withOS.withLinuxCustomImage(getCustomImage())
                        .withRootUsername(getAdminUserName());
                } else {
                    publicKeyManaged = withOS.withLinuxGalleryImageVersion(getGalleryImageVersion())
                        .withRootUsername(getAdminUserName());
                }

                if (!ObjectUtils.isBlank(getAdminPassword()) && !ObjectUtils.isBlank(getSsh())) {
                    createManaged = publicKeyManaged.withRootPassword(getAdminPassword()).withSsh(getSsh());
                } else if (!ObjectUtils.isBlank(getAdminPassword())) {
                    createManaged = publicKeyManaged.withRootPassword(getAdminPassword());
                } else {
                    createManaged = publicKeyManaged.withSsh(getSsh());
                }

            } else {
                managedCreate = withOS.withSpecializedOSDisk(
                    client.disks().getById(getDiskId()), OperatingSystemTypes.LINUX
                );
            }

            if (createUnmanaged != null) {
                create = createUnmanaged.withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            } else if (createManaged != null) {
                create = createManaged.withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            }
        } else {
            //windows
            WithWindowsCreateUnmanaged createUnmanaged = null;
            WithWindowsCreateManaged createManaged = null;
            if (isLatestPopularOrSpecific) {
                VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManagedOrUnmanaged managedOrUnmanaged;

                if (getVmImageType().equals("latest")) {
                    managedOrUnmanaged = withOS.withLatestWindowsImage(getImagePublisher(),getImageOffer(),getImageSku())
                        .withAdminUsername(getAdminUserName());
                } else if (getVmImageType().equals("popular")) {
                    managedOrUnmanaged = withOS.withPopularWindowsImage(
                        KnownWindowsVirtualMachineImage.valueOf(getKnownVirtualImage())
                    ).withAdminUsername(getAdminUserName());
                } else {
                    managedOrUnmanaged = withOS.withSpecificWindowsImageVersion(
                        client.virtualMachineImages()
                            .getImage(getImageRegion(),getImagePublisher(),getImageOffer(),getImageSku(),getImageVersion())
                            .imageReference()
                    ).withAdminUsername(getAdminUserName());
                }

                managedCreate = managedOrUnmanaged.withAdminPassword(getAdminPassword())
                    .withExistingDataDisk(client.disks().getById(getDiskId()));

            } else if (getVmImageType().equals("stored")) {
                createUnmanaged = withOS.withStoredWindowsImage(getStoredImage())
                    .withAdminUsername(getAdminUserName()).withAdminPassword(getAdminPassword());

            } else if (getVmImageType().equals("custom") || getVmImageType().equals("gallery")) {
                WithWindowsAdminPasswordManaged passwordManaged;
                if (getVmImageType().equals("custom")) {
                    passwordManaged = withOS.withWindowsCustomImage(getCustomImage())
                        .withAdminUsername(getAdminUserName());
                } else {
                    passwordManaged = withOS.withWindowsGalleryImageVersion(getGalleryImageVersion())
                        .withAdminUsername(getAdminUserName());
                }

                createManaged = passwordManaged.withAdminPassword(getAdminPassword());
            } else {
                managedCreate = withOS.withSpecializedOSDisk(
                    client.disks().getById(getDiskId()), OperatingSystemTypes.WINDOWS
                );
            }

            if (createUnmanaged != null) {
                create = createUnmanaged
                    .withoutAutoUpdate()
                    .withoutVMAgent()
                    .withTimeZone(getTimeZone())
                    .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            } else if (createManaged != null) {
                create = createManaged
                    .withoutAutoUpdate()
                    .withoutVMAgent()
                    .withTimeZone(getTimeZone())
                    .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            }
        }

        if (managedCreate != null) {
            create = managedCreate.withDataDiskDefaultCachingType(CachingTypes.fromString(getCachingType()))
                .withDataDiskDefaultStorageAccountType(StorageAccountTypes.fromString(getStorageAccountTypeDataDisk()))
                .withOSDiskStorageAccountType(StorageAccountTypes.fromString(getStorageAccountTypeOsDisk()))
                .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
        }

        if (create == null) {
            throw new BeamException("Invalid config.");
        }

        if (!getSecondaryNetworkInterfaceNames().isEmpty()) {
            for (String networkInterfaceName : getSecondaryNetworkInterfaceNames()) {
                create = create.withExistingSecondaryNetworkInterface(
                    client.networkInterfaces().getByResourceGroup(getResourceGroupName(), networkInterfaceName)
                );
            }
        }

        VirtualMachine virtualMachine = create.withTags(getTags()).create();

        setVirtualMachineId(virtualMachine.id());
        setVmId(virtualMachine.vmId());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        Azure client = createClient();

        VirtualMachine virtualMachine = client.virtualMachines().getById(getVirtualMachineId());

        virtualMachine.update()
            .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()))
            .withDataDiskDefaultCachingType(CachingTypes.fromString(getCachingType()))
            .withDataDiskDefaultStorageAccountType(StorageAccountTypes.fromString(getStorageAccountTypeDataDisk()))
            .withTags(getTags())
            .apply();
    }

    @Override
    public void delete() {
        Azure client = createClient();

        client.virtualMachines().deleteById(getVirtualMachineId());
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("virtual machine");

        if (!ObjectUtils.isBlank(getVirtualMachineName())) {
            sb.append(" - ").append(getVirtualMachineName());
        }

        if (!ObjectUtils.isBlank(getVirtualMachineId())) {
            sb.append(" - ").append(getVirtualMachineId());
        }

        return sb.toString();
    }
}
