package beam.openstack.config;

import beam.BeamResourceFilter;
import beam.BeamRuntime;
import com.psddev.dari.util.ObjectUtils;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupConfiguration;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenStackProjectFilter extends BeamResourceFilter {

    private Set<String> hostnames;

    public Set<String> getHostnames() {
        if (hostnames == null) {
            hostnames = new HashSet<>();
        }

        return hostnames;
    }

    public void setHostnames(Set<String> hostnames) {
        this.hostnames = hostnames;
    }

    @Override
    public boolean isInclude(Object osResource) {
        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();

        if (osResource instanceof Network) {
            String networkName = String.format("%s v%s",
                    runtime.getProject(),
                    runtime.getSerial());

            Network network = (Network) osResource;
            if (!network.getName().startsWith(networkName)) {
                return false;
            }
        } else if (osResource instanceof KeyPair) {
            return ((KeyPair) osResource).getName().startsWith(runtime.getProject() + '-');
        } else if (osResource instanceof LoadBalancer) {
            String lbName = String.format("%s v%s",
                    runtime.getProject(),
                    runtime.getSerial());

            LoadBalancer loadBalancer = (LoadBalancer) osResource;
            if (!loadBalancer.getName().endsWith(lbName)) {
                return false;
            }
        } else if (osResource instanceof Server) {
            Server server = (Server) osResource;
            Map<String, String> metadata = server.getMetadata();
            String layer = metadata.get("layer");

            if (ObjectUtils.isBlank(metadata)) {
                return false;
            }

            if (!runtime.getProject().equals(metadata.get("project")) ||
                    !runtime.getSerial().equals(metadata.get("serial"))) {
                return false;
            }

            if (!ObjectUtils.isBlank(layer) && getIncludedLayers().size() == 0) {
                return "gateway".equals(layer) || runtime.getEnvironment().equals(metadata.get("environment"));

            } else if (!ObjectUtils.isBlank(layer) && getIncludedLayers().contains(layer)) {
                return true;

            } else {
                return false;
            }
        } else if (osResource instanceof Group) {
            Group group = (Group) osResource;
            GroupConfiguration config = group.getGroupConfiguration();
            Map<String, String> metadata = config.getMetadata();

            if (ObjectUtils.isBlank(metadata)) {
                return false;
            }

            if (runtime.getProject().equals(metadata.get("project")) &&
                    runtime.getSerial().equals(metadata.get("serial")) &&
                    runtime.getEnvironment().equals(metadata.get("environment"))) {
                return true;
            } else {
                return false;
            }
        } else if (osResource instanceof RecordDetail) {
            RecordDetail record = (RecordDetail) osResource;

            if (!getHostnames().contains(record.getName())) {
                return false;
            }
        }

        return true;
    }

}