package beam.handlers;

import beam.config.RootConfig;
import com.amazonaws.util.IOUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.QueryParameterUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TicketAuthApiHandler implements HttpHandler {

    private RootConfig rootConfig;

    private final Map<String, String> settings = new ConcurrentHashMap<>();

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TicketAuthApiHandler.class);

    public TicketAuthApiHandler(RootConfig rootConfig) {
        this.rootConfig = rootConfig;
        this.settings.putAll(TicketAuthApiHandler.getSettings());
    }

    public static synchronized Map<String, String> getSettings() {
        Map<String, String> settings = new ConcurrentHashMap<>();

        File configFile = new File("/etc/beam/beam-auth.yml");
        if (configFile.exists()) {
            try (InputStream configInput = new FileInputStream(configFile)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) new Yaml().load(configInput);

                for (String key : config.keySet()) {
                    settings.put(key, config.get(key).toString());
                }
            } catch (IOException ioe) {

            }
        }

        return settings;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (settings.isEmpty()) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().send("");
            return;
        }

        try {
            exchange.startBlocking();

            String postData = IOUtils.toString(exchange.getInputStream());

            Map<String, Deque<String>> postParams = QueryParameterUtils.parseQueryString(postData, null);
            String username = postParams.get("username") != null ? postParams.get("username").element() : null;
            String password = postParams.get("password") != null ? postParams.get("password").element() : null;

            Map<String, Deque<String>> params = exchange.getQueryParameters();
            String back = params.get("back") != null ? params.get("back").element() : null;

            String html = IOUtils.toString(TicketAuthApiHandler.class.getResourceAsStream("/tkt-auth.html"));
            if (back == null) {
                html = IOUtils.toString(TicketAuthApiHandler.class.getResourceAsStream("/tkt-invalid.html"));
                exchange.getResponseSender().send(html);

                return;
            }

            if (username != null && password != null) {
                LdapContext context = createContext(settings.get("ldapProviderUrl"));
                if (ldapAuthenticate(context, username, password, settings.get("ldapPrincipalFormat")) ||
                        debugAuthenticate(username, password)) {
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");

                    String secret = settings.get("ticketSecret");

                    InetAddress addr = InetAddress.getByName("0.0.0.0");
                    sha.update(addr.getAddress());

                    int ts = (int) (System.currentTimeMillis() / 1000);
                    ByteBuffer tsBuf = ByteBuffer.allocate(4);
                    tsBuf.putInt(ts);
                    tsBuf.flip();
                    sha.update(tsBuf);

                    sha.update(secret.getBytes("UTF-8"));
                    sha.update(username.getBytes("UTF-8"));
                    sha.update("\0".getBytes());
                    sha.update("\0".getBytes());

                    byte[] digest0 = sha.digest();

                    MessageDigest digestSha = MessageDigest.getInstance("SHA-256");
                    digestSha.update(StringUtils.hex(digest0).getBytes());
                    digestSha.update(secret.getBytes("UTF-8"));

                    String digest = StringUtils.hex(digestSha.digest());
                    String cookie = digest + StringUtils.hex(tsBuf.array()) + username + "!";

                    HttpString redirect = new HttpString("Location");
                    exchange.setStatusCode(302);
                    exchange.getResponseHeaders().add(redirect, back + "?beam-sso=" + cookie);
                    exchange.getResponseSender().send("");
                    return;
                } else {
                    html = html.replace("${error}", "<div class='info-module-error'>Invalid credentials</div>");
                }
            } else {

                html = html.replace("${error}", "");
            }

            String name = rootConfig.getNetworkConfig().getName().replace("-", " ");
            String project = org.apache.commons.lang3.StringUtils.capitalize(name);
            html = html.replace("${project}", project);
            html = html.replace("${username}", username != null ? username : "");

            exchange.getResponseSender().send(html);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static LdapContext createContext(String providerUrl) {
        if (ObjectUtils.isBlank(providerUrl)) {
            return null;
        }

        Properties env = new Properties();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerUrl);

        try {
            return new InitialLdapContext(env, null);

        } catch (NamingException error) {
            LOGGER.warn("Can't connect to LDAP!", error);
            return null;
        }
    }

    public boolean debugAuthenticate(String username, String password) {
        String debugUser = settings.get("debugUser");
        String debugPassword = settings.get("debugPassword");

        if (debugUser == null || debugPassword == null ||
                username == null || password == null) {
            return false;
        }

        if (debugUser.equals(username) && debugPassword.equals(password)) {
            return true;
        }

        return false;
    }

    public boolean ldapAuthenticate(LdapContext context, String principal, String credentials, String principalFormat) {
        try {
            if (ObjectUtils.isBlank(principalFormat)) {
                return false;
            }

            try {
                principal = String.format(principalFormat, "\"" + principal.replace("\"", "\\\"")  + "\"");

                context.addToEnvironment("java.naming.ldap.factory.socket", "beam.handlers.TicketAuthApiHandler$CustomCaSocketFactory");
                context.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
                context.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials);

                // Force the reconnect to really authenticate using SSL.
                try {
                    context.reconnect(null);
                    return true;

                } catch (AuthenticationException error) {
                    LOGGER.debug("Authenticated failed.", error);
                    return false;
                }

            } finally {
                context.close();
            }

        } catch (Exception error) {
            LOGGER.warn("Can't read from LDAP!", error);
            return false;
        }
    }

    public Long ipToInt(String addr) {
        long ipAsLong = 0;
        for (String byteString : addr.split("\\."))
        {
            ipAsLong = (ipAsLong << 8) | Integer.parseInt(byteString);
        }

        return ipAsLong;
    }

    public static class CustomCaSocketFactory extends SocketFactory {

        private SSLSocketFactory socketFactory;

        public static SocketFactory getDefault() {
            return new CustomCaSocketFactory();
        }

        public CustomCaSocketFactory() {
            Map<String, String> settings = TicketAuthApiHandler.getSettings();
            try {
                String cert = IOUtils.toString(new FileInputStream(new File(settings.get("ldapCertificatePath"))));

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                InputStream caCertInput = new ByteArrayInputStream(cert.getBytes(StringUtils.UTF_8));
                Certificate caCert = CertificateFactory.getInstance("X.509").generateCertificate(caCertInput);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                SSLContext sslContext = SSLContext.getInstance("TLS");

                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", caCert);
                tmf.init(keyStore);
                sslContext.init(null, tmf.getTrustManagers(), null);

                socketFactory = sslContext.getSocketFactory();
            } catch (Exception ex)  {

            }
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return socketFactory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return socketFactory.createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return socketFactory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return socketFactory.createSocket(address, port, localAddress, localPort);
        }
    }

}