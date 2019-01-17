package beam.aws.elb;

import beam.aws.AwsResource;
import beam.aws.ec2.InstanceResource;
import beam.aws.iam.IamRoleResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Resource;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancing.model.HealthCheck;
import software.amazon.awssdk.services.elasticloadbalancing.model.Instance;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancing.model.ListenerDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::elastic-load-balancer elb-example
 *         load-balancer-name: "elb-example"
 *         security-groups: ["sg-3c0dfa46"]
 *         subnets: ["subnet-04d3e586552ea5fe1"]
 *         instances: ["i-01faa0ea54134538b"]
 *     end
 */
@ResourceName("load-balancer")
public class LoadBalancerResource extends AwsResource {

    public HealthCheckResource healthCheck;
    private List<String> instances;
    private List<ListenerResource> listener;
    private String loadBalancerName;
    private List<String> securityGroups;
    private List<String> subnets;

    @ResourceDiffProperty(nullable = true, subresource = true)
    public HealthCheckResource getHealthCheck(){
        return healthCheck;
    }

    public void setHealthCheck(HealthCheckResource healthCheck){
        this.healthCheck = healthCheck;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getInstances() {
        if (instances == null) {
            instances = new ArrayList<>();
        }

        return instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
    }

    @ResourceDiffProperty(nullable = true, subresource = true)
    public List<ListenerResource> getListener() {
        if (listener == null) {
            listener = new ArrayList<>();
        }

        return listener;
    }

    public void setListener(List<ListenerResource> listener) {
        this.listener = listener;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new ArrayList<>();
        }

        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }

    @ResourceDiffProperty(updatable = true)
    public List<String> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<String> subnets) {
        this.subnets = subnets;
    }

    public HealthCheckResource fromHealthCheck(HealthCheck healthCheck){
        HealthCheckResource healthCheckResource = new HealthCheckResource();
        healthCheckResource.setHealthyThreshold(healthCheck.healthyThreshold());
        healthCheckResource.setInterval(healthCheck.interval());
        healthCheckResource.setTarget(healthCheck.target());
        healthCheckResource.setTimeout(healthCheck.timeout());
        healthCheckResource.setUnhealthyThreshold(healthCheck.unhealthyThreshold());
        return healthCheckResource;
    }

    public List<Instance> toInstances(){
        List<Instance> instance = new ArrayList<>();
        for (String resource : getInstances()) {
            instance.add(Instance.builder().instanceId(resource).build());
        }
        return instance;
    }

    public List<String> fromInstances(List<Instance> instances){
        List<String> stringInstances = new ArrayList<>();
        for(Instance inst : instances){
            stringInstances.add(inst.instanceId());
        }
        return stringInstances;
    }

    public List<Listener> toListeners() {
        List<Listener> listeners = new ArrayList<>();
        for(ListenerResource resource : getListener()) {
            Listener newListener = Listener.builder()
                    .instancePort(resource.getInstancePort())
                    .instanceProtocol(resource.getInstanceProtocol())
                    .loadBalancerPort(resource.getLoadBalancerPort())
                    .protocol(resource.getProtocol())
                    //.sslCertificateId(resource.getSslCertificateId())
                    .build();
            listeners.add(newListener);
        }

        return listeners;
    }

    @Override
    public boolean refresh() {
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);

        DescribeLoadBalancersResponse response = client.describeLoadBalancers(r -> r.loadBalancerNames(getLoadBalancerName()));
        if (response != null) {
            for(LoadBalancerDescription description : response.loadBalancerDescriptions()){
                setLoadBalancerName(description.loadBalancerName());
                setInstances(fromInstances(description.instances()));
                setSecurityGroups(description.securityGroups());
                setSubnets(description.subnets());
                //setHealthCheck(fromHealthCheck(description.healthCheck()));

                getListener().clear();
                for (ListenerDescription listenerDescription : description.listenerDescriptions()){
                    Listener listener = listenerDescription.listener();
                    ListenerResource listenerResource = new ListenerResource();
                    listenerResource.setInstancePort(listener.instancePort());
                    listenerResource.setInstanceProtocol(listener.instanceProtocol());
                    listenerResource.setLoadBalancerPort(listener.loadBalancerPort());
                    listenerResource.setProtocol(listener.protocol());
                    getListener().add(listenerResource);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);

        client.createLoadBalancer(r -> r.listeners(toListeners())
                .securityGroups(getSecurityGroups())
                .subnets(getSubnets())
                .loadBalancerName(getLoadBalancerName())
        );

        client.registerInstancesWithLoadBalancer(r -> r.instances(toInstances())
                                    .loadBalancerName(getLoadBalancerName()));
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);

        LoadBalancerResource currentResource = (LoadBalancerResource) current;

        List<Instance> instanceAdditions = new ArrayList<>(toInstances());
        instanceAdditions.removeAll(currentResource.toInstances());

        List<Instance> instanceSubtractions = new ArrayList<>(currentResource.toInstances());
        instanceSubtractions.removeAll(toInstances());

        List<String> subnetAdditions = new ArrayList<>(getSubnets());
        subnetAdditions.removeAll(currentResource.getSubnets());

        List<String> subnetSubtractions = new ArrayList<>(currentResource.getSubnets());
        subnetSubtractions.removeAll(getSubnets());

        List<String> sgAdditions = new ArrayList<>(getSecurityGroups());
        sgAdditions.removeAll(currentResource.getSecurityGroups());

        if (!instanceAdditions.isEmpty()) {
            client.registerInstancesWithLoadBalancer(r -> r.instances(instanceAdditions)
            .loadBalancerName(getLoadBalancerName()));
        }
        if (!instanceSubtractions.isEmpty()) {
            client.deregisterInstancesFromLoadBalancer(r -> r.instances(instanceSubtractions)
            .loadBalancerName(getLoadBalancerName()));
        }

        client.attachLoadBalancerToSubnets(r -> r.subnets(subnetAdditions)
        .loadBalancerName(getLoadBalancerName()));

        client.detachLoadBalancerFromSubnets(r -> r.subnets(subnetSubtractions)
        .loadBalancerName(getLoadBalancerName()));

        if(!sgAdditions.isEmpty()) {
            client.applySecurityGroupsToLoadBalancer(r -> r.securityGroups(sgAdditions)
                    .loadBalancerName(getLoadBalancerName()));
        }

    }

    @Override
    public void delete() {
        ElasticLoadBalancingClient client = createClient(ElasticLoadBalancingClient.class);
        client.deleteLoadBalancer(r -> r.loadBalancerName(getLoadBalancerName()));
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getLoadBalancerName() != null) {
            sb.append("load balancer " + getLoadBalancerName());

        } else {
            sb.append("load balancer ");
        }

        return sb.toString();
    }
}
