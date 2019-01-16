package beam.aws.elb;

import beam.aws.AwsResource;
import beam.aws.ec2.InstanceResource;
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
 *         security-groups: ["security-group"]
 *         subnets: ["10.0.0.0/24"]
 *     end
 */
@ResourceName("load-balancer")
public class LoadBalancerResource extends AwsResource {

    private String loadBalancerName;
    private List<String> instances;
    private List<ListenerResource> listeners;
    private List<String> securityGroups;
    public HealthCheckResource healthCheck;
    private List<String> subnets;

    public HealthCheckResource getHealthCheck(){
        return healthCheck;
    }

    public void setHealthCheck(HealthCheckResource healthCheck){
        this.healthCheck = healthCheck;
    }

    public List<String> getInstances() {
        if (instances == null) {
            instances = new ArrayList<>();
        }

        return instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
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

    public List<ListenerResource> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        return listeners;
    }

    public void setListeners(List<ListenerResource> listeners) {
        this.listeners = listeners;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public void setLoadBalancerName(String loadBalancerName) {
        this.loadBalancerName = loadBalancerName;
    }

    public List<String> getSecurityGroups() {
        if (securityGroups == null) {
            securityGroups = new ArrayList<>();
        }

        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }


    public List<String> getSubnets() {
        if (subnets == null) {
            subnets = new ArrayList<>();
        }

        return subnets;
    }

    public void setSubnets(List<String> subnets) {
        this.subnets = subnets;
    }

    public List<Listener> toListeners() {
        List<Listener> listeners = new ArrayList<>();
        for(ListenerResource resource : getListeners()) {
            Listener newListener = Listener.builder()
                    .instancePort(resource.getInstancePort())
                    .instanceProtocol(resource.getInstanceProtocol())
                    .loadBalancerPort(resource.getLoadBalancerPort())
                    .protocol(resource.getProtocol())
                    .sslCertificateId(resource.getSslCertificateId())
                    .build();
            listeners.add(newListener);
        }

        return listeners;
    }

    /*
    public HealthCheckResource fromHealthCheck(HealthCheck healthCheck){
        HealthCheckResource healthCheckResource = new HealthCheckResource();
        healthCheckResource.setUnhealthyThreshold(healthCheck.unhealthyThreshold());
        healthCheckResource.setInterval(healthCheck.interval());
        healthCheckResource.setTarget(healthCheck.target());
        healthCheckResource.setTimeout(healthCheck.timeout());
        healthCheckResource.setUnhealthyThreshold(healthCheck.unhealthyThreshold());
        return healthCheckResource;
    }*/

    @Override
    public boolean refresh() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .region(Region.US_EAST_1)
                .build();

        DescribeLoadBalancersResponse response = client.describeLoadBalancers(r -> r.loadBalancerNames(getLoadBalancerName()));
        if (response != null) {
            for(LoadBalancerDescription description : response.loadBalancerDescriptions()){
                setLoadBalancerName(description.loadBalancerName());
                setInstances(fromInstances(description.instances()));
                setSecurityGroups(description.securityGroups());
                setSubnets(description.subnets());
                //refresh for health check will happen in its own class
                //not sure if that is correct
                //refresh listeners
                getListeners().clear();
                for (ListenerDescription listenerDescription : description.listenerDescriptions()){
                    Listener listener = listenerDescription.listener();
                    ListenerResource listenerResource = new ListenerResource();
                    listenerResource.setInstancePort(listener.instancePort());
                    listenerResource.setInstanceProtocol(listener.instanceProtocol());
                    listenerResource.setLoadBalancerPort(listener.loadBalancerPort());
                    listenerResource.setProtocol(listener.protocol());
                    getListeners().add(listenerResource);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .region(Region.US_EAST_1)
                .build();

        System.out.println("Show security groups "+getSecurityGroups());
        System.out.println("Show subnets "+getSubnets());
        System.out.println("Show listeners "+getListeners());
        System.out.println("Show health check "+getHealthCheck());

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
        //do same thing with list, update and remove
        //deleteLoadBalancerListeners
        //deregisterInstancesFromLoadBalancer
        //detachLoadBalancerFromSubnets


    }

    @Override
    public void delete() {
        ElasticLoadBalancingClient client = ElasticLoadBalancingClient.builder()
                .region(Region.US_EAST_1)
                .build();

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
