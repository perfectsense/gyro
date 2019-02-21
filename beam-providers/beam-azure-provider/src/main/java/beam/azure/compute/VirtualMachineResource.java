package beam.azure.compute;

import beam.azure.AzureResource;
import beam.lang.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.CachingTypes;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.StorageAccountTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPrivateIP;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithPublicIPAddress;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithOS;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithManagedCreate;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsCreateUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsCreateManaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsCreateManagedOrUnmanaged;
import com.microsoft.azure.management.compute.VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManaged;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.compute.WinRMListener;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.psddev.dari.util.ObjectUtils;

import java.util.Set;

public class VirtualMachineResource extends AzureResource {
    private String name;
    private String resourceGroupName;
    private String networkId;
    private String adminUserName;
    private String adminPassword;
    private String virtualMachineId;
    private String vmId;
    private String publicIpAddress;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getVmImageType() {
        return vmImageType;
    }

    public void setVmImageType(String vmImageType) {
        this.vmImageType = vmImageType;
    }

    public String getSsh() {
        return ssh;
    }

    public void setSsh(String ssh) {
        this.ssh = ssh;
    }

    public String getStoredImage() {
        return storedImage;
    }

    public void setStoredImage(String storedImage) {
        this.storedImage = storedImage;
    }

    public String getCustomImage() {
        return customImage;
    }

    public void setCustomImage(String customImage) {
        this.customImage = customImage;
    }

    public String getGalleryImageVersion() {
        return galleryImageVersion;
    }

    public void setGalleryImageVersion(String galleryImageVersion) {
        this.galleryImageVersion = galleryImageVersion;
    }

    public String getCachingType() {
        return cachingType;
    }

    public void setCachingType(String cachingType) {
        this.cachingType = cachingType;
    }

    public String getStorageAccountTypeDataDisk() {
        return storageAccountTypeDataDisk;
    }

    public void setStorageAccountTypeDataDisk(String storageAccountTypeDataDisk) {
        this.storageAccountTypeDataDisk = storageAccountTypeDataDisk;
    }

    public String getStorageAccountTypeOsDisk() {
        return storageAccountTypeOsDisk;
    }

    public void setStorageAccountTypeOsDisk(String storageAccountTypeOsDisk) {
        this.storageAccountTypeOsDisk = storageAccountTypeOsDisk;
    }

    public String getVmSizeType() {
        return vmSizeType;
    }

    public void setVmSizeType(String vmSizeType) {
        this.vmSizeType = vmSizeType;
    }

    public String getKnownVirtualImage() {
        return knownVirtualImage;
    }

    public void setKnownVirtualImage(String knownVirtualImage) {
        this.knownVirtualImage = knownVirtualImage;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public boolean refresh() {
        Azure client = createClient();

        VirtualMachine virtualMachine = client.virtualMachines().getById(getVirtualMachineId());

        setName(virtualMachine.name());

        return true;
    }

    @Override
    public void create() {
        Azure client = createClient();

        WithPrivateIP withPrivateIP = client.virtualMachines().define(getName())
            .withRegion(Region.fromName(getRegion()))
            .withExistingResourceGroup(getResourceGroupName())
            .withExistingPrimaryNetwork(client.networks().getById(getNetworkId()))
            .withSubnet(getSubnet());

        WithPublicIPAddress withPublicIpAddress;
        if (!ObjectUtils.isBlank(getPrivateIpAddress())) {
            withPublicIpAddress = withPrivateIP.withPrimaryPrivateIPAddressStatic(getPrivateIpAddress());
        } else {
            withPublicIpAddress = withPrivateIP.withPrimaryPrivateIPAddressDynamic();
        }

        WithOS withOS;
        if (!ObjectUtils.isBlank(getPublicIpAddress())) {
            withOS = withPublicIpAddress.withoutPrimaryPublicIPAddress();
        } else {
            withOS = withPublicIpAddress.withExistingPrimaryPublicIPAddress(
                client.publicIPAddresses().getById(getPublicIpAddress())
            );
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
                    managedOrUnmanaged = withOS.withLatestLinuxImage("","","")
                        .withRootUsername(getAdminUserName());
                } else if (getVmImageType().equals("popular")) {
                    managedOrUnmanaged = withOS.withPopularLinuxImage(
                        KnownLinuxVirtualMachineImage.valueOf(getKnownVirtualImage())
                    ).withRootUsername(getAdminUserName());
                } else {
                    managedOrUnmanaged = withOS.withSpecificLinuxImageVersion(
                        client.virtualMachineImages()
                            .getImage("region","publisher","offer","sku","version")
                            .imageReference()
                    ).withRootUsername(getAdminUserName());
                }

                createManagedOrUnmanaged = !ObjectUtils.isBlank(getAdminPassword())
                    ? managedOrUnmanaged.withRootPassword(getAdminPassword()) : null;

                createManagedOrUnmanaged = !ObjectUtils.isBlank(getSsh())
                    ? (createManagedOrUnmanaged != null
                        ? createManagedOrUnmanaged.withSsh(getSsh()) : managedOrUnmanaged.withSsh(getSsh())) : null;

                managedCreate = createManagedOrUnmanaged != null
                    ? createManagedOrUnmanaged.withExistingDataDisk(client.disks().getById(getDiskId())) : null;


            } else if (getVmImageType().equals("stored")) {
                WithLinuxRootPasswordOrPublicKeyUnmanaged publicKeyUnmanaged = withOS
                    .withStoredLinuxImage(getStoredImage())
                    .withRootUsername(getAdminUserName());

                createUnmanaged = !ObjectUtils.isBlank(getAdminPassword())
                    ? publicKeyUnmanaged.withRootPassword(getAdminPassword()) : null;

                createUnmanaged = !ObjectUtils.isBlank(getSsh())
                    ? (createUnmanaged != null
                        ? createUnmanaged.withSsh(getSsh()) : publicKeyUnmanaged.withSsh(getSsh())) : null;

            } else if (getVmImageType().equals("custom") || getVmImageType().equals("gallery")) {
                WithLinuxRootPasswordOrPublicKeyManaged publicKeyManaged;

                if (getVmImageType().equals("custom")) {
                    publicKeyManaged = withOS.withLinuxCustomImage(getCustomImage())
                        .withRootUsername(getAdminUserName());
                } else {
                    publicKeyManaged = withOS.withLinuxGalleryImageVersion(getGalleryImageVersion())
                        .withRootUsername(getAdminUserName());
                }

                createManaged = !ObjectUtils.isBlank(getAdminPassword())
                    ? publicKeyManaged.withRootPassword(getAdminPassword()) : null;

                createManaged = !ObjectUtils.isBlank(getSsh())
                    ? (createManaged != null
                        ? createManaged.withSsh(getSsh()) : publicKeyManaged.withSsh(getSsh())) : null;

            } else {
                managedCreate = withOS.withSpecializedOSDisk(
                    client.disks().getById(getDiskId()), OperatingSystemTypes.LINUX
                );
            }

            if (createUnmanaged != null) {
                create = createUnmanaged.withSize(VirtualMachineSizeTypes.fromString(""));
            } else if (createManaged != null) {
                create = createManaged.withSize(VirtualMachineSizeTypes.fromString(""));
            }
        } else {
            //windows
            WithWindowsCreateUnmanaged createUnmanaged = null;
            WithWindowsCreateManaged createManaged = null;
            if (isLatestPopularOrSpecific) {
                VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManagedOrUnmanaged managedOrUnmanaged;

                if (getVmImageType().equals("latest")) {
                    managedOrUnmanaged = withOS.withLatestWindowsImage("publisher","offer","sku")
                        .withAdminUsername(getAdminUserName());
                } else if (getVmImageType().equals("popular")) {
                    managedOrUnmanaged = withOS.withPopularWindowsImage(
                        KnownWindowsVirtualMachineImage.valueOf(getKnownVirtualImage())
                    ).withAdminUsername(getAdminUserName());
                } else {
                    managedOrUnmanaged = withOS.withSpecificWindowsImageVersion(
                        client.virtualMachineImages()
                            .getImage("region","publisher","offer","sku","version")
                            .imageReference()
                    ).withAdminUsername(getAdminUserName());
                }

                WithWindowsCreateManagedOrUnmanaged createManagedOrUnmanaged = !ObjectUtils.isBlank(getAdminPassword())
                    ? managedOrUnmanaged.withAdminPassword(getAdminPassword()) : null;

                managedCreate = createManagedOrUnmanaged != null
                    ? createManagedOrUnmanaged.withExistingDataDisk(client.disks().getById(getDiskId())) : null;

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

            WinRMListener d = new WinRMListener();
            if (createUnmanaged != null) {
                create = createUnmanaged
                    .withoutAutoUpdate()
                    .withoutVMAgent()
                    .withTimeZone(getTimeZone())
                    .withWinRM(d)
                    .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            } else if (createManaged != null) {
                create = createManaged
                    .withoutAutoUpdate()
                    .withoutVMAgent()
                    .withTimeZone(getTimeZone())
                    .withWinRM(d)
                    .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
            }
        }

        if (managedCreate != null) {
            create = managedCreate.withDataDiskDefaultCachingType(CachingTypes.fromString(getCachingType()))
                .withDataDiskDefaultStorageAccountType(StorageAccountTypes.fromString(getStorageAccountTypeDataDisk()))
                .withOSDiskStorageAccountType(StorageAccountTypes.fromString(getStorageAccountTypeOsDisk()))
                .withSize(VirtualMachineSizeTypes.fromString(getVmSizeType()));
        }

        VirtualMachine virtualMachine = create.create();

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

        if (!ObjectUtils.isBlank(getName())) {
            sb.append(" - ").append(getName());
        }

        if (!ObjectUtils.isBlank(getVirtualMachineId())) {
            sb.append(" - ").append(getVirtualMachineId());
        }

        return sb.toString();
    }
}
