package beam.azure;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import beam.BeamInstance;
import beam.BeamRuntime;
import com.google.common.io.BaseEncoding;
import com.microsoft.azure.management.compute.models.InstanceViewStatus;
import com.microsoft.azure.management.compute.models.NetworkInterfaceReference;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterfaceOperations;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.PublicIpAddressOperations;
import com.microsoft.azure.management.network.models.NetworkInterface;
import com.microsoft.azure.management.network.models.PublicIpAddress;
import com.microsoft.azure.management.network.models.ResourceId;
import com.psddev.dari.util.ObjectUtils;

public class AzureInstance extends BeamInstance {

    private final AzureCloud cloud;
    private final VirtualMachine instance;
    private Boolean sandboxed;

    public AzureInstance(AzureCloud cloud, VirtualMachine instance) {
        this.cloud = cloud;
        this.instance = instance;

        isSandboxed();
    }

    public VirtualMachine getNativeInstance() {
        return instance;
    }

    @Override
    public String getId() {
        return instance.getName();
    }

    @Override
    public String getEnvironment() {
        return instance.getTags().get("beam.env");
    }

    @Override
    public String getLocation() {
        return instance.getLocation();
    }

    @Override
    public String getRegion() {
        return instance.getLocation();
    }

    @Override
    public String getLayer() {
        return instance.getTags().get("beam.layer");
    }

    @Override
    public String getState() {
        try {
            for (InstanceViewStatus status : instance.getInstanceView().getStatuses()) {
                if (status.getDisplayStatus().startsWith("VM")) {
                    return status.getDisplayStatus();
                }
            }

        } catch (Exception error) {
        }

        return null;
    }

    @Override
    public boolean isSandboxed() {
        if (sandboxed == null) {
            String userdata = instance.getOSProfile().getCustomData();

            if (userdata != null) {
                String userdataJson = new String(BaseEncoding.base64().decode(userdata));
                Map<String, String> userdataMap = (Map<String, String>) ObjectUtils.fromJson(userdataJson);

                if ("true".equals(userdataMap.get("sandbox"))) {
                    sandboxed = true;
                } else {
                    sandboxed = false;
                }
            }
        }

        if (sandboxed == null) {
           sandboxed = true;
        }

        return sandboxed;
    }

    @Override
    public Set<String> getServices() {
        return null;
    }

    @Override
    public String getPublicIpAddress() {
        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), getRegion());

        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkInterfaceOperations nIOperations = client.getNetworkInterfacesOperations();
        PublicIpAddressOperations pIAOperations = client.getPublicIpAddressesOperations();

        for (NetworkInterfaceReference nicReference : instance.getNetworkProfile().getNetworkInterfaces()) {
            String nicUri = nicReference.getReferenceUri();
            String[] uriParts = nicUri.split("/");
            String nicName = uriParts[uriParts.length-1];

            try {
                NetworkInterface nic = nIOperations.get(resourceGroup, nicName).getNetworkInterface();
                ResourceId resourceId  = nic.getIpConfigurations().get(0).getPublicIpAddress();
                if (resourceId == null) {
                    return null;

                } else {
                    String publicIpId = resourceId.getId();
                    String[] idParts = publicIpId.split("/");
                    String publicIpName = idParts[idParts.length-1];
                    PublicIpAddress address = pIAOperations.get(resourceGroup, publicIpName).getPublicIpAddress();
                    return address.getIpAddress();
                }

            } catch (Exception error) {
            }
        }

        return null;
    }

    @Override
    public String getPrivateIpAddress() {
        String privateIp = null;
        String resourceGroup = String.format("%s-%s", BeamRuntime.getCurrentRuntime().getProject(), getRegion());

        NetworkResourceProviderClient client = cloud.createNetworkManagementClient();
        NetworkInterfaceOperations nIOperations = client.getNetworkInterfacesOperations();

        for (NetworkInterfaceReference nicReference : instance.getNetworkProfile().getNetworkInterfaces()) {
            String nicUri = nicReference.getReferenceUri();
            String[] uriParts = nicUri.split("/");
            String nicName = uriParts[uriParts.length-1];

            try {
                NetworkInterface nic = nIOperations.get(resourceGroup, nicName).getNetworkInterface();
                privateIp = nic.getIpConfigurations().get(0).getPrivateIpAddress();

            } catch (Exception error) {
            }
        }

        return privateIp;
    }

    @Override
    public Date getDate() {
        return new Date();
    }
}
