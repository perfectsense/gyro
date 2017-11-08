package beam.enterprise;

import beam.Beam;
import beam.BeamConfig;
import beam.BeamException;
import beam.BeamRuntime;
import beam.twofactor.TwoFactorProvider;
import beam.twofactor.TwoFactorProviderName;
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

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnterpriseApi {

    /**
     * Returns {@code true} if Beam Enterprise APIs can be called.
     */
    public static boolean isAvailable(String projectName) {
        Map<String, Object> settings = getPerProjectSettings(projectName);
        if (settings.get("enterprise") != null && settings.get("enterprise").equals(Boolean.FALSE)) {
            return false;
        }

        return !ObjectUtils.isBlank(settings.get("enterpriseUrl")) ||
                !ObjectUtils.isBlank(BeamConfig.get(String.class, "enterpriseUrl", null));
    }

    public static boolean isAvailable() {
        String projectName = BeamRuntime.getCurrentRuntime().getProject();
        return isAvailable(projectName);
    }

    private static String createEnterpriseUrl() {
        String projectName = BeamRuntime.getCurrentRuntime().getProject();
        Map<String, Object> settings = getPerProjectSettings(projectName);
        String projectEnterpriseUrl = settings.get("enterpriseUrl") != null ? settings.get("enterpriseUrl").toString() : null;
        projectEnterpriseUrl = StringUtils.removeEnd(projectEnterpriseUrl, "/");
        String globalEnterpriseUrl = StringUtils.removeEnd(BeamConfig.get(String.class, "enterpriseUrl", null), "/");
        return projectEnterpriseUrl != null ? projectEnterpriseUrl : globalEnterpriseUrl;
    }

    public static String getEnterpriseUser() {
        String projectName = BeamRuntime.getCurrentRuntime().getProject();
        Map<String, Object> settings = getPerProjectSettings(projectName);
        String projectEnterpriseLogin = settings.get("enterpriseUser") != null ? settings.get("enterpriseUser").toString() : null;
        String globalEnterpriseLogin = BeamConfig.getLogin();
        return projectEnterpriseLogin != null ? projectEnterpriseLogin : globalEnterpriseLogin;
    }

    private static Map<String, Object> getPerProjectSettings(String projectName) {
        Map<String, Object> settings = new HashMap<>();
        List<Map<String, Object>> projects = BeamConfig.get(List.class, "projects", new ArrayList<>());
        for (Map<String, Object> project : projects) {
            if (projectName.equals(project.get("name"))) {
                settings.put("enterprise", project.get("enterprise"));
                settings.put("enterpriseUrl", project.get("enterpriseUrl"));
                settings.put("enterpriseUser", project.get("enterpriseUser"));
            }
        }

        return settings;
    }

    /**
     * Prepares a file with the given {@code name} in the local cache
     * directory.
     *
     * @param name Can't be blank.
     * @return Never {@code null}.
     * @throws IOException If the local cache directory can't be created.
     */
    public static File prepareLocalFile(String name) throws IOException {
        ErrorUtils.errorIfBlank(name, "name");

        File file = Paths.get(
                BeamConfig.getBeamUserHome(),
                ".beam",
                "enterprise",
                StringUtils.encodeUri(createEnterpriseUrl()),
                name).
                toFile();

        IoUtils.createParentDirectories(file);

        return file;
    }

    private static String createEndpointUrl(String endpoint) {
        return createEnterpriseUrl()  + StringUtils.ensureStart(endpoint, "/");
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
    public static Map<String, Object> callUnauthenticated(String endpoint, NameValuePair... parameters) throws IOException {
        ErrorUtils.errorIfBlank(endpoint, "endpoint");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpUriRequest request = RequestBuilder.post().
                    setUri(createEndpointUrl(endpoint)).
                    addParameters(parameters).
                    build();

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
    public static Map<String, Object> call(String endpoint, NameValuePair... parameters) throws IOException {
        for (int i = 0; i < 3; ++ i) {
            String sessionId = null;
            File sessionFile = prepareLocalFile("session");
            Console console = System.console();

            // Try to reuse existing session.
            if (sessionFile.exists()) {
                sessionId = IoUtils.toString(sessionFile, Charsets.UTF_8);

            } else {

                // Not authenticated yet so ask for login and password.
                String login = getEnterpriseUser();
                String authenticateUrl = createEndpointUrl("/authenticate");

                if (ObjectUtils.isBlank(login)) {
                    console.printf(String.format(
                            "Authenticating against Beam Enterprise at %s\n",
                            authenticateUrl));

                    while (ObjectUtils.isBlank(login)) {
                        console.printf("Login: ");

                        login = console.readLine().trim();
                    }

                } else {
                    console.printf(String.format(
                            "Authenticating as %s against Beam Enterprise at %s\n",
                            login,
                            authenticateUrl));
                }

                for (int j = 0; j < 3; ++ j) {
                    console.printf("Password: ");
                    String password = new String(console.readPassword()).trim();

                    Map<String, Object> authMap = callUnauthenticated(
                            "authenticate",
                            new BasicNameValuePair("login", login),
                            new BasicNameValuePair("password", password));

                    String status = ObjectUtils.to(String.class, authMap.get("status"));
                    sessionId = ObjectUtils.to(String.class, authMap.get("sessionId"));

                    // Authentication successful so cache the session.
                    if ("ok".equals(status)) {
                        console.printf("Login successful!\n");

                        sessionFile.delete();

                        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sessionFile), Charsets.UTF_8)) {
                            Files.setPosixFilePermissions(sessionFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                            writer.write(sessionId);
                        }

                        break;
                    } else if ("requires_2fa_enrollment".equals(status)) {
                        String provider = ObjectUtils.to(String.class, authMap.get("provider"));

                        for (Class<?> subClass : Beam.reflections.getSubTypesOf(TwoFactorProvider.class)) {
                            TwoFactorProviderName providerName = subClass.getAnnotation(TwoFactorProviderName.class);

                            if (providerName != null && providerName.value().equals(provider)) {
                                try {
                                    TwoFactorProvider tfp = (TwoFactorProvider) subClass.newInstance();
                                    tfp.setSessionId(sessionId);
                                    tfp.handleNeeds2FAEnrollment(authMap);
                                } catch (IllegalAccessException | InstantiationException ex) {
                                }

                                break;
                            }
                        }

                        break;
                    } else if ("requires_2fa".equals(status)) {
                        String provider = ObjectUtils.to(String.class, authMap.get("provider"));

                        for (Class<?> subClass : Beam.reflections.getSubTypesOf(TwoFactorProvider.class)) {
                            TwoFactorProviderName providerName = subClass.getAnnotation(TwoFactorProviderName.class);

                            if (providerName != null && providerName.value().equals(provider)) {
                                try {
                                    TwoFactorProvider tfp = (TwoFactorProvider) subClass.newInstance();
                                    tfp.setSessionId(sessionId);
                                    tfp.handleNeeds2FA(authMap);

                                    if (tfp.isSuccess()) {
                                        sessionFile.delete();

                                        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sessionFile), Charsets.UTF_8)) {
                                            Files.setPosixFilePermissions(sessionFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                                            writer.write(sessionId);
                                        }
                                    } else {
                                        throw new BeamException("Second factor verification failed.");
                                    }
                                } catch (IllegalAccessException | InstantiationException ex) {
                                }

                                break;
                            }

                        }

                        break;
                    } else if ("no-person".equals(status)) {
                        console.printf("Login unsuccessful!\n");

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
            if ("no-person".equals(status) ||
                    "no-session".equals(status) ||
                    "session-expired".equals(status)) {

                console.printf("Session invalid!\n");
                sessionFile.delete();
                continue;
            }

            return responseMap;
        }

        throw new BeamException("Login unsuccessful 3 times!");
    }
}
