package gyro.aws.elbv2;

import gyro.aws.AwsResource;
import gyro.core.diff.ResourceDiffProperty;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenerCertificatesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ListenerNotFoundException;

import java.util.ArrayList;
import java.util.List;

public abstract class ListenerResource extends AwsResource {

    private List<CertificateResource> certificate;
    private String defaultCertificate;
    private String listenerArn;
    private String loadBalancerArn;
    private Integer port;
    private String protocol;
    private String sslPolicy;

    /**
     *  List of certificates associated with the listener (Optional)
     */
    @ResourceDiffProperty(updatable = true, subresource = true, nullable = true)
    public List<CertificateResource> getCertificate() {
        if (certificate == null) {
            certificate = new ArrayList<>();
        }

        return certificate;
    }

    public void setCertificate(List<CertificateResource> certificate) {
        this.certificate = certificate;
    }

    /**
     *  The default certificate ARN associated with the listener (Optional)
     */
    @ResourceDiffProperty(updatable = true, nullable = true)
    public String getDefaultCertificate() {
        return defaultCertificate;
    }

    public void setDefaultCertificate(String defaultCertificate) {
        this.defaultCertificate = defaultCertificate;
    }

    public String getListenerArn() {
        return listenerArn;
    }

    public void setListenerArn(String listenerArn) {
        this.listenerArn = listenerArn;
    }

    public String getLoadBalancerArn() {
        return loadBalancerArn;
    }

    public void setLoadBalancerArn(String loadBalancerArn) {
        this.loadBalancerArn = loadBalancerArn;
    }

    /**
     *  Connection port between client and the load balancer (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     *  Connection protocol between client and the load balancer (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     *  Security policy that defines supported protocols and ciphers (Optional)
     */
    @ResourceDiffProperty(updatable = true)
    public String getSslPolicy() {
        return sslPolicy;
    }

    public void setSslPolicy(String sslPolicy) {
        this.sslPolicy = sslPolicy;
    }

    public Listener internalRefresh() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        try {
            DescribeListenersResponse lstResponse = client.describeListeners(r -> r.listenerArns(getListenerArn()));

            Listener listener = lstResponse.listeners().get(0);

            if (listener.certificates().size() > 0) {
                setDefaultCertificate(listener.certificates().get(0).certificateArn());
            }
            setListenerArn(listener.listenerArn());
            setLoadBalancerArn(listener.loadBalancerArn());
            setPort(listener.port());
            setProtocol(listener.protocolAsString());
            setSslPolicy(listener.sslPolicy());

            if (this instanceof ApplicationLoadBalancerListenerResource) {
                getCertificate().clear();
                DescribeListenerCertificatesResponse certResponse = client.describeListenerCertificates(r -> r.listenerArn(getListenerArn()));
                if (certResponse != null) {
                    for (Certificate certificate : certResponse.certificates()) {
                        if (!certificate.isDefault()) {
                            CertificateResource cert = new CertificateResource();
                            cert.setCertificateArn(certificate.certificateArn());
                            cert.setIsDefault(certificate.isDefault());
                            cert.parent(this);
                            getCertificate().add(cert);
                        }
                    }
                }
            }

            return listener;

        } catch (ListenerNotFoundException ex) {
            return null;
        }
    }

    @Override
    public void delete() {
        ElasticLoadBalancingV2Client client = createClient(ElasticLoadBalancingV2Client.class);
        client.deleteListener(r -> r.listenerArn(getListenerArn()));
    }

    public List<Certificate> toCertificates() {
        List<Certificate> certificates = new ArrayList<>();
        for (CertificateResource cert : getCertificate()) {
            certificates.add(Certificate.builder().certificateArn(cert.getCertificateArn()).build());
        }
        return certificates;
    }
}
