package beam.openstack;

import beam.core.BeamException;
import beam.lang.Resource;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import java.io.Closeable;
import java.util.Properties;

public abstract class OpenstackResource extends Resource {
    private Closeable client;
    private String region;

    @Override
    public Class resourceCredentialsClass() {
        return OpenstackCredentials.class;
    }

    protected <T extends Closeable> T createClient(Class<T> clientClass) {
        if (client == null) {
            try {
                switch (clientClass.getName().split("\\.")[clientClass.getName().split("\\.").length - 1]) {
                    case "AutoscaleApi": client = createContextBuilder("rackspace-autoscale-us").buildApi(clientClass);
                    break;
                    case "CloudFilesApi": client = createContextBuilder("rackspace-cloudfiles-us").buildApi(clientClass);
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
}
