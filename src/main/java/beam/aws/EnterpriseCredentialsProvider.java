package beam.aws;

import beam.BeamException;
import beam.enterprise.EnterpriseApi;
import beam.enterprise.EnterpriseException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.common.base.Charsets;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link AWSCredentialsProvider} implementation that returns temporary
 * credentials using Beam Enterprise sessions.
 */
public class EnterpriseCredentialsProvider implements AWSCredentialsProvider {

    private final String accountName;
    private final String projectName;
    private boolean extended;

    private final transient Lazy<AWSCredentials> credentials = new Lazy<AWSCredentials>() {

        @Override
        @SuppressWarnings("unchecked")
        protected AWSCredentials create() {
            try {
                File credsFile = EnterpriseApi.prepareLocalFile("aws-temporary-credentials-" + accountName + "-" + projectName);

                if (credsFile.exists()) {
                    Map<String, Object> credsMap = (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(credsFile, Charsets.UTF_8));

                    // Don't use the cached credentials if it's going to
                    // expire within the next 1 minute.
                    if (ObjectUtils.to(long.class, credsMap.get("expiration")) > (System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1))) {
                        return new EnterpriseCredentials(credsMap);
                    }
                }

                // Exchange session for temporary credentials.
                Map<String, Object> exchangeMap = EnterpriseApi.call(
                        "aws/exchange-session",
                        new BasicNameValuePair("accountName", accountName),
                        new BasicNameValuePair("projectName", projectName),
                        new BasicNameValuePair("extended", Boolean.toString(extended)));

                switch (ObjectUtils.to(String.class, exchangeMap.get("status"))) {
                    case "ok" :
                        @SuppressWarnings("unchecked")
                        Map<String, Object> credsMap = (Map<String, Object>) exchangeMap.get("credentials");

                        if (!extended) {
                            credsFile.delete();

                            try (Writer writer = new OutputStreamWriter(new FileOutputStream(credsFile), Charsets.UTF_8)) {
                                Files.setPosixFilePermissions(credsFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                                writer.write(ObjectUtils.toJson(credsMap));
                            }
                        }

                        return new EnterpriseCredentials(credsMap);

                    // Errors fetching the temporary credentials.
                    case "no-account" :
                        throw new BeamException(String.format(
                                "[%s] account doesn't exist!",
                                accountName), null, "no-account");

                    case "no-project" :
                        throw new BeamException(String.format(
                                "[%s] project doesn't exist!",
                                projectName), null, "no-project");

                    case "no-account-project" :
                        throw new BeamException(String.format(
                                "[%s] project can't access [%s] account!",
                                projectName,
                                accountName), null, "no-account-project");

                    case "no-access" :
                        throw new BeamException(String.format(
                                "You do not have access to the %s project!",
                                projectName), null, "no-access");

                    default :
                        throw new EnterpriseException(exchangeMap);
                }

            } catch (IOException error) {

            }

            return null;
        }
    };

    /**
     * Creates an instance.
     *
     * @param accountName Can't be blank.
     * @param projectName Can't be blank.
     */
    public EnterpriseCredentialsProvider(String accountName, String projectName) {
        ErrorUtils.errorIfBlank(accountName, "accountName");
        ErrorUtils.errorIfBlank(projectName, "projectName");

        this.accountName = accountName;
        this.projectName = projectName;
    }

    @Override
    public AWSCredentials getCredentials() {
        return credentials.get();
    }

    @Override
    public void refresh() {
        try {
            File credsFile = EnterpriseApi.prepareLocalFile("aws-temporary-credentials-" + accountName + "-" + projectName);
            credsFile.delete();
        } catch (IOException ioe) {
        }

        credentials.reset();
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }
}
