package beam.plugins.enterprise;

import beam.aws.AwsCredentials;
import beam.core.diff.ResourceName;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

@ResourceName("enterprise-aws-credentials")
public class EnterpriseAwsCredentials extends AwsCredentials {

    private AwsCredentialsProvider provider;
    private EnterpriseApi api;
    private String enterpriseUrl;
    private String project;
    private String account;

    @Override
    public AwsCredentialsProvider provider() {
        if (provider == null) {
            provider = new EnterpriseCredentialsProviderChain(
                new EnterpriseCredentialsProvider(api(), getAccount(), getProject()),
                ProfileCredentialsProvider.create(getProfileName()),
                InstanceProfileCredentialsProvider.create());
        }

        return provider;
    }

    public EnterpriseApi api() {
        if (api == null) {
            String login = EnterpriseConfig.get(String.class, getProject(), "login", System.getProperty("user.name"));
            String duoPush = EnterpriseConfig.get(String.class, getProject(), "duo-push", "none");

            api = new EnterpriseApi(getEnterpriseUrl(), login, duoPush);
        }

        return api;
    }

    public String getEnterpriseUrl() {
        return enterpriseUrl;
    }

    public void setEnterpriseUrl(String enterpriseUrl) {
        this.enterpriseUrl = enterpriseUrl;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

}
