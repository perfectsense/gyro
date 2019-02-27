package gyro.plugin.enterprise;

import beam.core.BeamCore;
import beam.core.BeamException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;

public class EnterpriseApi {

    private String enterpriseUrl;
    private String enterpriseUser;
    private String duoPush;

    public EnterpriseApi(String enterpriseUrl, String enterpriseUser, String duoPush) {
        this.enterpriseUrl = enterpriseUrl;
        this.enterpriseUser = enterpriseUser;
        this.duoPush = duoPush;
    }

    private String enterpriseUrl() {
        return enterpriseUrl;
    }

    public String enterpriseUser() {
        return enterpriseUser;
    }

    public String duoPush() {
        return duoPush;
    }

    /**
     * Prepares a file with the given {@code name} in the local cache
     * directory.
     *
     * @param name Can't be blank.
     * @return Never {@code null}.
     * @throws IOException If the local cache directory can't be created.
     */
    public File prepareLocalFile(String name) throws IOException {
        ErrorUtils.errorIfBlank(name, "name");

        File file = Paths.get(EnterpriseConfig.getUserHome(), ".beam", "enterprise", StringUtils.encodeUri(enterpriseUrl()), name).toFile();
        IoUtils.createParentDirectories(file);

        return file;
    }

    private String createEndpointUrl(String endpoint) {
        return enterpriseUrl()  + StringUtils.ensureStart(endpoint, "/");
    }

    /**
     * Calls the API at the {@given endpoint} with the given
     * {@code parameters} without authenticating.
     *
     * @param endpoint Can't be blank.
     * @param parameters May be {@code null}.
     * @return Never {@code null}.
     * @throws IOException If the network fails during the API call.
     */
    public Map<String, Object> callUnauthenticated(String endpoint, NameValuePair... parameters) throws IOException {
        ErrorUtils.errorIfBlank(endpoint, "endpoint");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpUriRequest request = RequestBuilder.post()
                .setUri(createEndpointUrl(endpoint))
                .addParameters(parameters)
                .build();

            try (CloseableHttpResponse response = client.execute(request)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) ObjectUtils.fromJson(
                    EntityUtils.toString(response.getEntity(), Charsets.UTF_8));

                if (responseMap == null) {
                    responseMap = ImmutableMap.of(
                        "status", "error",
                        "code", response.getStatusLine().getStatusCode(),
                        "reason", response.getStatusLine().getReasonPhrase()
                    );
                }

                if ("error".equals(responseMap.get("status"))) {
                    throw new EnterpriseException(responseMap);
                }

                return responseMap;
            }
        }
    }

    /**
     * Calls the API at the {@given endpoint} with the given
     * {@code parameters} after authenticating.
     *
     * @param endpoint Can't be blank.
     * @param parameters May be {@code null}.
     * @return Never {@code null}.
     * @throws IOException If the network fails during the API call.
     */
    public Map<String, Object> call(String endpoint, NameValuePair... parameters) throws IOException {
        for (int i = 0; i < 3; ++ i) {
            String sessionId = null;
            File sessionFile = prepareLocalFile("session");

            // Try to reuse existing session.
            if (sessionFile.exists()) {
                sessionId = IoUtils.toString(sessionFile, Charsets.UTF_8);

            } else {

                // Not authenticated yet so ask for login and password.
                String login = enterpriseUser();
                String authenticateUrl = createEndpointUrl("/authenticate");

                if (ObjectUtils.isBlank(login)) {
                    BeamCore.ui().write(
                        "Authenticating against Beam Enterprise at %s\n",
                        authenticateUrl);

                    while (ObjectUtils.isBlank(login)) {
                        login = BeamCore.ui().readText("Login: ");
                    }

                } else {
                    BeamCore.ui().write(
                        "Authenticating as %s against Beam Enterprise at %s\n",
                        login,
                        authenticateUrl);
                }

                for (int j = 0; j < 3; ++ j) {
                    String password = BeamCore.ui().readPassword("Password: ");

                    Map<String, Object> authMap = callUnauthenticated(
                        "authenticate",
                        new BasicNameValuePair("login", login),
                        new BasicNameValuePair("password", password));

                    String status = ObjectUtils.to(String.class, authMap.get("status"));
                    sessionId = ObjectUtils.to(String.class, authMap.get("sessionId"));

                    // Authentication successful so cache the session.
                    if ("ok".equals(status)) {
                        BeamCore.ui().write("Login successful!\n");

                        sessionFile.delete();

                        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sessionFile), Charsets.UTF_8)) {
                            Files.setPosixFilePermissions(sessionFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                            writer.write(sessionId);
                        }

                        break;
                    } else if ("requires_2fa_enrollment".equals(status)) {
                        DuoApi duo = new DuoApi(this);
                        duo.setDuoPush(duoPush());
                        duo.setSessionId(sessionId);
                        duo.handleNeedsTwoFactorEnrollment(authMap);

                        break;
                    } else if ("requires_2fa".equals(status)) {
                        DuoApi duo = new DuoApi(this);
                        duo.setDuoPush(duoPush());
                        duo.setSessionId(sessionId);
                        duo.handleNeeds2FA(authMap);

                        if (duo.isSuccess()) {
                            sessionFile.delete();

                            try (Writer writer = new OutputStreamWriter(new FileOutputStream(sessionFile), Charsets.UTF_8)) {
                                Files.setPosixFilePermissions(sessionFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                                writer.write(sessionId);
                            }
                        } else {
                            throw new BeamException("Second factor verification failed.");
                        }

                        break;
                    } else if ("no-person".equals(status)) {
                        BeamCore.ui().write("Login unsuccessful!\n");

                    } else {
                        throw new EnterpriseException(authMap);
                    }
                }
            }

            if (sessionId == null) {
                throw new BeamException("Login unsuccessful 3 times!");
            }

            // The real API call using the session.
            int parametersLength = parameters != null ? parameters.length : 0;
            NameValuePair[] newParameters = new NameValuePair[parametersLength + 1];

            if (parametersLength > 0) {
                System.arraycopy(parameters, 0, newParameters, 0, parametersLength);
            }

            newParameters[parametersLength] = new BasicNameValuePair("sessionId", sessionId);

            Map<String, Object> responseMap = callUnauthenticated(endpoint, newParameters);
            Object status = responseMap.get("status");

            // Session's been invalidated?
            if ("no-person".equals(status) || "no-session".equals(status) || "session-expired".equals(status)) {
                BeamCore.ui().write("Session invalid!\n");
                sessionFile.delete();
                continue;
            }

            return responseMap;
        }

        throw new BeamException("Login unsuccessful 3 times!");
    }

}
