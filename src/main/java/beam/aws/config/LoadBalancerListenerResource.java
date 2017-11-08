package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;

import beam.BeamException;
import beam.BeamReference;
import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;
import beam.diff.NullArrayList;
import beam.diff.ResourceDiffProperty;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancerPoliciesRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerPolicyRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerPolicyRequest;
import com.amazonaws.services.elasticloadbalancing.model.PolicyDescription;
import com.amazonaws.services.elasticloadbalancing.model.PolicyAttributeDescription;
import com.amazonaws.services.elasticloadbalancing.model.PolicyAttribute;
import com.amazonaws.services.elasticloadbalancing.model.SetLoadBalancerPoliciesOfListenerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.PolicyNotFoundException;

public class LoadBalancerListenerResource extends AWSResource<Listener> {

    private Integer instancePort;
    private String instanceProtocol;
    private BeamReference loadBalancer;
    private Integer loadBalancerPort;
    private String protocol;
    private BeamReference sslCertificate;
    private String predefinedPolicy;
    private String serverOrderPreference;
    private List<String> sslProtocols;
    private List<String> sslCiphers;
    private List<String> policyNames;
    private Long stickyDuration;

    @ResourceDiffProperty
    public Integer getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(Integer instancePort) {
        this.instancePort = instancePort;
    }

    @ResourceDiffProperty
    public String getInstanceProtocol() {
        return instanceProtocol != null ? instanceProtocol.toUpperCase(Locale.ENGLISH) : null;
    }

    public void setInstanceProtocol(String instanceProtocol) {
        this.instanceProtocol = instanceProtocol;
    }

    public BeamReference getLoadBalancer() {
        return newParentReference(LoadBalancerResource.class, loadBalancer);
    }

    public void setLoadBalancer(BeamReference loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @ResourceDiffProperty
    public Integer getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(Integer loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    @ResourceDiffProperty
    public String getProtocol() {
        return protocol != null ? protocol.toUpperCase(Locale.ENGLISH) : null;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ResourceDiffProperty (updatable = true)
    public BeamReference getSslCertificate() {
        return sslCertificate;
    }

    public void setSslCertificate(BeamReference sslCertificate) {
        this.sslCertificate = sslCertificate;
    }

    @ResourceDiffProperty (updatable = true)
    public String getPredefinedPolicy() {
        return predefinedPolicy;
    }

    public void setPredefinedPolicy(String predefinedPolicy) {
        this.predefinedPolicy = predefinedPolicy;
    }

    @ResourceDiffProperty (updatable = true)
    public String getServerOrderPreference() {
        return serverOrderPreference;
    }

    public void setServerOrderPreference(String serverOrderPreference) {
        this.serverOrderPreference = serverOrderPreference;
    }

    @ResourceDiffProperty (updatable = true)
    public List<String> getSslProtocols() {
        if (sslProtocols == null) {
            sslProtocols = new NullArrayList<>();
        }
        return sslProtocols;
    }

    public void setSslProtocols(List<String> sslProtocols) {
        this.sslProtocols = sslProtocols;
    }

    @ResourceDiffProperty (updatable = true)
    public List<String> getSslCiphers() {
        if (sslCiphers == null) {
            sslCiphers = new NullArrayList<>();
        }
        return sslCiphers;
    }

    public void setSslCiphers(List<String> sslCiphers) {
        this.sslCiphers = sslCiphers;
    }

    public List<String> getPolicyNames() {
        if (policyNames == null) {
            policyNames = new ArrayList<>();
        }
        return policyNames;
    }

    public void setPolicyNames(List<String> policyNames) {
        this.policyNames = policyNames;
    }

    public Listener toListener() {
        Listener listener = new Listener();

        listener.setInstancePort(getInstancePort());
        listener.setInstanceProtocol(getInstanceProtocol());
        listener.setLoadBalancerPort(getLoadBalancerPort());
        listener.setProtocol(getProtocol());

        BeamReference cert = getSslCertificate();

        if (cert != null) {
            listener.setSSLCertificateId(cert.awsId());
        }

        return listener;
    }

    @ResourceDiffProperty (updatable = true)
    public Long getStickyDuration() {
        return stickyDuration;
    }

    public void setStickyDuration(Long stickyDuration) {
        this.stickyDuration = stickyDuration;
    }

    public String getStickyPolicyName(String elbName) {
        String stickySessionPolicy = "beam-" + elbName + "-" +
                getLoadBalancerPort() + "-" + getProtocol();
        return stickySessionPolicy;
    }

    @Override
    public List<Object> diffIds() {
        return Arrays.asList(
                getLoadBalancer(),
                getLoadBalancerPort(),
                getProtocol());
    }

    public void setPredefinedPolicyDetails(AWSCloud cloud) {
        List<String> sslProtocols = new ArrayList<>();
        List<String> sslCiphers = new ArrayList<>();
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        DescribeLoadBalancerPoliciesRequest dlbpRequest = new DescribeLoadBalancerPoliciesRequest();

        dlbpRequest.setPolicyNames(Arrays.asList(getPredefinedPolicy()));

        if (client.describeLoadBalancerPolicies(dlbpRequest).getPolicyDescriptions().size() != 1) {
            throw new BeamException(String.format(
                    "The predefined policy %s does not exist!", getPredefinedPolicy()));
        }

        setServerOrderPreference("false");
        PolicyDescription policyDescription = client.describeLoadBalancerPolicies(dlbpRequest).getPolicyDescriptions().get(0);
        for (PolicyAttributeDescription policyAttributeDescription : policyDescription.getPolicyAttributeDescriptions()) {
            if (policyAttributeDescription.getAttributeValue().equals("true")) {
                String attributeName = policyAttributeDescription.getAttributeName();
                if (attributeName.startsWith("Protocol-")) {
                    sslProtocols.add(attributeName);
                } else if (attributeName.equals("Server-Defined-Cipher-Order")) {
                    setServerOrderPreference("true");
                } else {
                    sslCiphers.add(attributeName);
                }
            }
        }
        setSslProtocols(sslProtocols);
        setSslCiphers(sslCiphers);
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, Listener listener) {
        setInstancePort(listener.getInstancePort());
        setInstanceProtocol(listener.getInstanceProtocol());
        setLoadBalancerPort(listener.getLoadBalancerPort());
        setProtocol(listener.getProtocol());
        setSslCertificate(newReference(ServerCertificateResource.class, listener.getSSLCertificateId()));

        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());

        List<String> sslProtocols = new ArrayList<>();
        List<String> sslCiphers = new ArrayList<>();
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource)getLoadBalancer().resolve();

        String stickySessionPolicy = getStickyPolicyName(loadBalancerResource.getLoadBalancerName());

        DescribeLoadBalancerPoliciesRequest dlbpRequest = new DescribeLoadBalancerPoliciesRequest();
        dlbpRequest.setPolicyNames(Arrays.asList(stickySessionPolicy));
        dlbpRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());

        try {
            PolicyDescription policyDescription = client.describeLoadBalancerPolicies(dlbpRequest).getPolicyDescriptions().get(0);
            if ("LBCookieStickinessPolicyType".equals(policyDescription.getPolicyTypeName())) {
                for (PolicyAttributeDescription attribute : policyDescription.getPolicyAttributeDescriptions()) {
                    if ("CookieExpirationPeriod".equals(attribute.getAttributeName())) {
                        setStickyDuration(Long.parseLong(attribute.getAttributeValue()));
                    }
                }

            } else {
                setStickyDuration(-1l);
            }

        } catch (PolicyNotFoundException error) {
            setStickyDuration(-1l);
        }

        getPolicyNames().remove(stickySessionPolicy);
        if (!getPolicyNames().isEmpty()) {
            setServerOrderPreference("false");
            dlbpRequest = new DescribeLoadBalancerPoliciesRequest();
            dlbpRequest.setPolicyNames(getPolicyNames());
            dlbpRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());

            if (client.describeLoadBalancerPolicies(dlbpRequest).getPolicyDescriptions().size() != 1) {
                throw new BeamException("One listener should only have one policy!");
            }

            PolicyDescription policyDescription = client.describeLoadBalancerPolicies(dlbpRequest).getPolicyDescriptions().get(0);
            boolean predefined = false;
            if (policyDescription.getPolicyName().startsWith("ELBSecurityPolicy-")) {
                setPredefinedPolicy(policyDescription.getPolicyName());
                predefined = true;
            } else if (policyDescription.getPolicyTypeName().equals("SSLNegotiationPolicyType")) {
                for (PolicyAttributeDescription policyAttributeDescription : policyDescription.getPolicyAttributeDescriptions()) {
                    if (policyAttributeDescription.getAttributeName().equals("Reference-Security-Policy")) {
                        setPredefinedPolicy(policyAttributeDescription.getAttributeValue());
                        predefined = true;
                    } else if (!predefined && policyAttributeDescription.getAttributeValue().equals("true")) {
                        String attributeName = policyAttributeDescription.getAttributeName();
                        if (attributeName.startsWith("Protocol-")) {
                            sslProtocols.add(attributeName);
                        } else if (attributeName.equals("Server-Defined-Cipher-Order")) {
                            setServerOrderPreference("true");
                        } else {
                            sslCiphers.add(attributeName);
                        }
                    }
                }
            }

            if (predefined) {
                setPredefinedPolicyDetails(cloud);
            } else {
                setPredefinedPolicy("Custom");
                setSslProtocols(sslProtocols);
                setSslCiphers(sslCiphers);
            }
        }
    }

    @Override
    public void create(AWSCloud cloud) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        CreateLoadBalancerListenersRequest clblRequest = new CreateLoadBalancerListenersRequest();

        clblRequest.setListeners(Arrays.asList(toListener()));
        clblRequest.setLoadBalancerName(getLoadBalancer().awsId());
        client.createLoadBalancerListeners(clblRequest);

        createPolicies(client);
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, Listener> current, Set<String> changedProperties) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        ((LoadBalancerListenerResource) current).deletePolicies(client);
        createPolicies(client);
    }

    @Override
    public void delete(AWSCloud cloud) {
        AmazonElasticLoadBalancingClient client = createClient(AmazonElasticLoadBalancingClient.class, cloud.getProvider());
        deletePolicies(client);

        DeleteLoadBalancerListenersRequest dlblRequest = new DeleteLoadBalancerListenersRequest();
        dlblRequest.setLoadBalancerName(getLoadBalancer().awsId());
        dlblRequest.setLoadBalancerPorts(Arrays.asList(getLoadBalancerPort()));
        client.deleteLoadBalancerListeners(dlblRequest);
    }

    public void createPolicies(AmazonElasticLoadBalancingClient client) {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource)getLoadBalancer().resolve();
        List<String> policyList = new ArrayList<>();
        if(getPredefinedPolicy() != null) {
            List<PolicyAttribute> attributes = new ArrayList<>();

            if (!getPredefinedPolicy().equals("Custom")) {
                attributes.add(new PolicyAttribute("Reference-Security-Policy", getPredefinedPolicy()));
            } else {
                for (String sslProtocol : getSslProtocols()) {
                    attributes.add(new PolicyAttribute(sslProtocol, "true"));
                }

                attributes.add(new PolicyAttribute("Server-Defined-Cipher-Order", getServerOrderPreference()));

                for (String sslCipher : getSslCiphers()) {
                    attributes.add(new PolicyAttribute(sslCipher, "true"));
                }
            }

            CreateLoadBalancerPolicyRequest clbpRequest = new CreateLoadBalancerPolicyRequest();
            clbpRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());
            clbpRequest.setPolicyAttributes(attributes);

            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SS").format(new Date());
            timeStamp = timeStamp.replace(".", "");
            String policyName = "beam-" + "SSLNegotiationPolicy-" + loadBalancerResource.getLoadBalancerName() + "-" + timeStamp;
            clbpRequest.setPolicyName(policyName);
            clbpRequest.setPolicyTypeName("SSLNegotiationPolicyType");
            client.createLoadBalancerPolicy(clbpRequest);

            policyList.add(policyName);
            setPolicyNames(policyList);
        }

        if (getStickyDuration() != null && getStickyDuration() >= 0) {
            List<PolicyAttribute> attributes = new ArrayList<>();
            if (getStickyDuration() > 0) {
                attributes.add(new PolicyAttribute("CookieExpirationPeriod", String.valueOf(getStickyDuration())));
            }

            CreateLoadBalancerPolicyRequest clbpRequest = new CreateLoadBalancerPolicyRequest();
            clbpRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());
            clbpRequest.setPolicyAttributes(attributes);

            String policyName = getStickyPolicyName(loadBalancerResource.getLoadBalancerName());
            clbpRequest.setPolicyName(policyName);
            clbpRequest.setPolicyTypeName("LBCookieStickinessPolicyType");
            client.createLoadBalancerPolicy(clbpRequest);

            policyList.add(policyName);
        }

        client.setLoadBalancerPoliciesOfListener(new SetLoadBalancerPoliciesOfListenerRequest(loadBalancerResource.getLoadBalancerName(), getLoadBalancerPort(), policyList));
    }

    public void deletePolicies(AmazonElasticLoadBalancingClient client) {
        LoadBalancerResource loadBalancerResource = (LoadBalancerResource) getLoadBalancer().resolve();
        client.setLoadBalancerPoliciesOfListener(new SetLoadBalancerPoliciesOfListenerRequest(loadBalancerResource.getLoadBalancerName(), getLoadBalancerPort(), new ArrayList<>()));
        if (!getPolicyNames().isEmpty() && getPredefinedPolicy().equals("Custom")) {
            for (String policy : getPolicyNames()) {
                DeleteLoadBalancerPolicyRequest deletePolicyRequest = new DeleteLoadBalancerPolicyRequest();
                deletePolicyRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());
                deletePolicyRequest.setPolicyName(policy);
                client.deleteLoadBalancerPolicy(deletePolicyRequest);
            }
        }

        if (getStickyDuration() != null && getStickyDuration() >= 0) {
            DeleteLoadBalancerPolicyRequest deletePolicyRequest = new DeleteLoadBalancerPolicyRequest();
            deletePolicyRequest.setLoadBalancerName(loadBalancerResource.getLoadBalancerName());
            deletePolicyRequest.setPolicyName(getStickyPolicyName(loadBalancerResource.getLoadBalancerName()));
            client.deleteLoadBalancerPolicy(deletePolicyRequest);
        }
    }

    @Override
    public String toDisplayString() {
        return String.format(
                "load balancer listener %s:%d/%s:%d",
                getProtocol(),
                getLoadBalancerPort(),
                getInstanceProtocol(),
                getInstancePort());
    }
}
