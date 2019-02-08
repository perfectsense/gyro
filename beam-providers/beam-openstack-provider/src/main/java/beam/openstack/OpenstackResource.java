package beam.openstack;

import beam.core.BeamException;
import beam.lang.Resource;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

public abstract class OpenstackResource extends Resource {
    private Closeable client;
    private String region;

    private static final Map<String, String> clientProviderMap = ImmutableMap.<String, String>builder()
        .put("AutoscaleApi", "rackspace-autoscale-us")
        .put("CloudFilesApi", "rackspace-cloudfiles-us")
        .put("NeutronApi", "rackspace-cloudnetworks-us")
        .put("NovaApi", "rackspace-cloudservers-us")
        .put("CinderApi", "rackspace-cloudblockstorage-us")
        .put("TroveApi", "rackspace-clouddatabases-us")
        .put("CloudLoadBalancersApi", "rackspace-cloudloadbalancers-us")
        .put("CloudDNSApi", "rackspace-clouddns-us")
        .put("PoppyApi", "rackspace-cdn-us")
        .build();

    @Override
    public Class resourceCredentialsClass() {
        return OpenstackCredentials.class;
    }

    protected abstract Class<? extends Closeable> getParentClientClass();

    protected <T> T createClient(Class<T> clientClass) {
        try {
            if (client == null) {
                if (clientProviderMap.containsKey(getClassName(getParentClientClass()))) {
                    client = createContextBuilder(clientProviderMap.get(getClassName(getParentClientClass()))).buildApi(getParentClientClass());
                } else {
                    throw new BeamException("Undefined class in scope of generating client.");
                }
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
