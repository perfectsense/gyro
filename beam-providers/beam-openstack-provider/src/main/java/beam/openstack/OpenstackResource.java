package beam.openstack;

import beam.core.BeamException;
import beam.lang.Resource;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Properties;

public abstract class OpenstackResource extends Resource {
    private Closeable client;
    private String region;

    @Override
    public Class resourceCredentialsClass() {
        return OpenstackCredentials.class;
    }

    protected abstract Class<? extends Closeable> getParentClientClass();

    protected <T> T createClient(Class<T> clientClass) {
        try {
            if (client == null) {
                createParentClient(getParentClientClass());
            }

            Class<?>[] paramTypes = {String.class};
            Method method = client.getClass().getMethod("get" + getClassName(clientClass), paramTypes);
            Object result = method.invoke(client, getRegion());

            return (T) result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private <T extends Closeable> T createParentClient(Class<T> clientClass) {
        if (client == null) {
            try {
                switch (getClassName(clientClass)) {
                    case "AutoscaleApi": client = createContextBuilder("rackspace-autoscale-us").buildApi(clientClass);
                    break;
                    case "CloudFilesApi": client = createContextBuilder("rackspace-cloudfiles-us").buildApi(clientClass);
                    break;
                    case "NeutronApi": client = createContextBuilder("rackspace-cloudnetworks-us").buildApi(clientClass);
                    break;
                    case "NovaApi": client = createContextBuilder("rackspace-cloudservers-us").buildApi(clientClass);
                    break;
                    case "CinderApi": client = createContextBuilder("rackspace-cloudblockstorage-us").buildApi(clientClass);
                    break;
                    case "TroveApi": client = createContextBuilder("rackspace-clouddatabases-us").buildApi(clientClass);
                    break;
                    case "CloudLoadBalancersApi": client = createContextBuilder("rackspace-cloudloadbalancers-us").buildApi(clientClass);
                    break;
                    case "CloudDNSApi": client = createContextBuilder("rackspace-clouddns-us").buildApi(clientClass);
                    break;
                    case "PoppyApi": client = createContextBuilder("rackspace-cdn-us").buildApi(clientClass);
                    break;
                    default: throw new BeamException("Undefined class in scope of generating client.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return (T) client;
    }

    private ContextBuilder createContextBuilder(String providerOrApi) {
        OpenstackCredentials credentials = (OpenstackCredentials) getResourceCredentials();
        setRegion(credentials.getRegion());
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(providerOrApi)
            .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()));

        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "50000");
        overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, "50000");
        contextBuilder.credentials(credentials.getUserName(), credentials.getApiKey());

        contextBuilder.overrides(overrides);

        return contextBuilder;
    }

    public String getRegion() {
        if (region == null) {
            OpenstackCredentials credentials = (OpenstackCredentials) getResourceCredentials();
            setRegion(credentials != null ? credentials.getRegion() : null);
        }

        return region;
    }

    private void setRegion(String region) {
        this.region = region;
    }

    private String getClassName(Class clientClass) {
        return clientClass.getName().split("\\.")[clientClass.getName().split("\\.").length - 1];
    }
}
