package beam.aws.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import beam.BeamResource;
import beam.BeamResourceFilter;
import beam.aws.AWSCloud;

import com.amazonaws.services.identitymanagement.model.ServerCertificate;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;

public class ServerCertificateResource extends AWSResource<ServerCertificate> {

    private String arn;
    private String serverCertificateName;

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getServerCertificateName() {
        return serverCertificateName;
    }

    public void setServerCertificateName(String serverCertificateName) {
        this.serverCertificateName = serverCertificateName;
    }

    @Override
    public String awsId() {
        return getArn();
    }

    @Override
    public List<String> diffIds() {
        return Arrays.asList(getArn());
    }

    @Override
    public void init(AWSCloud cloud, BeamResourceFilter filter, ServerCertificate cert) {
        ServerCertificateMetadata metadata = cert.getServerCertificateMetadata();

        setArn(metadata.getArn());
        setServerCertificateName(metadata.getServerCertificateName());
    }

    @Override
    public void create(AWSCloud cloud) {
    }

    @Override
    public void update(AWSCloud cloud, BeamResource<AWSCloud, ServerCertificate> current, Set<String> changedProperties) {
    }

    @Override
    public void delete(AWSCloud cloud) {
    }

    @Override
    public String toDisplayString() {
        return "server certificate " + getServerCertificateName();
    }
}
