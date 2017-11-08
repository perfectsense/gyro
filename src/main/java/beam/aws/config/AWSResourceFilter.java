package beam.aws.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beam.BeamRuntime;
import beam.BeamResourceFilter;
import beam.config.ConfigValue;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.identitymanagement.model.Role;
import com.psddev.dari.util.ObjectUtils;

@ConfigValue("psd")
public class AWSResourceFilter extends BeamResourceFilter {

    @Override
    public boolean isInclude(Object awsResource) {
        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        if (awsResource instanceof AutoScalingGroup) {
            List<TagDescription> tags = ((AutoScalingGroup) awsResource).getTags();
            String layer = getAutoScalingTagValue(tags, "beam.layer");

            if (!runtime.getProject().equals(getAutoScalingTagValue(tags, "beam.project")) ||
                    !runtime.getSerial().equals(getAutoScalingTagValue(tags, "beam.serial"))) {
                return false;
            }

            if (!ObjectUtils.isBlank(layer) && (getIncludedLayers().size() == 0 || getIncludedLayers().contains(layer))) {
                return "gateway".equals(layer) || runtime.getEnvironment().equals(getAutoScalingTagValue(tags, "beam.env"));

            } else {
                return false;
            }

        } else if (awsResource instanceof Instance) {
            List<Tag> tags = ((Instance) awsResource).getTags();
            String layer = getEC2TagValue(tags, "beam.layer");

            if (!ObjectUtils.isBlank(layer) && (getIncludedLayers().size() == 0 || getIncludedLayers().contains(layer))) {
                return "gateway".equals(layer) || runtime.getEnvironment().equals(getEC2TagValue(tags, "beam.env"));

            } else {
                return false;
            }

        } else if (awsResource instanceof KeyPairInfo) {
            return ((KeyPairInfo) awsResource).getKeyName().startsWith(runtime.getProject() + '-');

        } else if (awsResource instanceof Role) {
            return ((Role) awsResource).getRoleName().startsWith(runtime.getProject() + '-');

        } else if (awsResource instanceof Vpc) {
            List<Tag> tags = ((Vpc) awsResource).getTags();

            return runtime.getProject().equals(getEC2TagValue(tags, "beam.project")) &&
                    runtime.getSerial().equals(getEC2TagValue(tags, "beam.serial"));
        } else if (awsResource instanceof RouteTable &&
                (getIncludedLayers().size() != 0 && !getIncludedLayers().contains("gateway"))) {
            return false;
        } else if (awsResource instanceof Route) {
            Route route = (Route) awsResource;

            if (route.getGatewayId() != null && route.getGatewayId().startsWith("vgw")) {
                return false;

            } else if (route.getVpcPeeringConnectionId() != null) {
                return false;
            }
        }

        return true;
    }

    private String getEC2TagValue(List<Tag> tags, String key) {
        if (tags != null) {
            for (Tag tag : tags) {
                if (tag.getKey().equals(key)) {
                    return tag.getValue();
                }
            }
        }

        return null;
    }

    private String getAutoScalingTagValue(List<TagDescription> tags, String key) {
        if (tags != null) {
            for (TagDescription tag : tags) {
                if (tag.getKey().equals(key)) {
                    return tag.getValue();
                }
            }
        }

        return null;
    }

}
