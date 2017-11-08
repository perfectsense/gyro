package beam.azure.config;

import beam.BeamResourceFilter;
import beam.BeamRuntime;
import com.microsoft.azure.management.compute.models.VirtualMachine;
import com.microsoft.azure.management.dns.models.RecordSet;
import com.psddev.dari.util.ObjectUtils;

import java.util.Map;

public class AzureResourceFilter extends BeamResourceFilter {
    @Override
    public boolean isInclude(Object azureResource) {
        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        if (azureResource instanceof VirtualMachine) {
            Map<String, String> tags = ((VirtualMachine) azureResource).getTags();
            String layer = tags.get("beam.layer");

            if (!ObjectUtils.isBlank(layer) && getIncludedLayers().size() == 0) {
                return "gateway".equals(layer) || runtime.getEnvironment().equals(tags.get("beam.env"));

            } else if (!ObjectUtils.isBlank(layer) && getIncludedLayers().contains(layer)) {
                return true;

            } else {
                return false;
            }

        } else if (azureResource instanceof RecordSet) {
            Map<String, String> tags = ((RecordSet) azureResource).getTags();
            String layer = tags.get("beam.layer");

            if (!ObjectUtils.isBlank(layer) && getIncludedLayers().size() == 0) {
                return "gateway".equals(layer) || "loadBalancer".equals(layer) || runtime.getEnvironment().equals(tags.get("beam.env"));

            } else if (!ObjectUtils.isBlank(layer) && getIncludedLayers().contains(layer)) {
                return true;

            } else {
                return false;
            }

        }

        return true;
    }
}
