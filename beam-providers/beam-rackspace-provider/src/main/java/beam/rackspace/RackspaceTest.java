package beam.rackspace;

import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.Apis;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.Authentication;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.providers.Providers;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;

import org.jclouds.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@ResourceName("aloha")
public class RackspaceTest extends RackspaceResource {
    /*public RackspaceTest() {
        AutoscaleApi autoscaleApi = ContextBuilder.newBuilder("rackspace-autoscale-us")
            .credentials("{username}", "{apiKey}")
            .buildApi(AutoscaleApi.class);
    }*/

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    private ContextBuilder createContextBuilder(String providerOrApi) {
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(providerOrApi).
            modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()));

        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "20000");
        overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, "20000");
        contextBuilder.credentials("beam-perfectsense-deepanjan@brightspot.com", "fdb709a02ea946f58f4c3d84013fc0bc");

        contextBuilder.overrides(overrides);

        return contextBuilder;
    }

    public NovaApi abc(String[] args) {
        // The provider configures jclouds To use the Rackspace Cloud (US)
        // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudservers-uk"
        String provider = System.getProperty("provider.cs", "rackspace-cloudservers-us");

        String username = args[0];
        String credential = args[1];

        Properties overrides = new Properties();

        if (args.length == 3 && "password".equals(args[2])) {
            overrides.put(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        }

        return ContextBuilder.newBuilder(provider)
            .credentials(username, credential)
            .overrides(overrides)
            .buildApi(NovaApi.class);
    }

    @Override
    public void create() {
        List<String> names = new ArrayList<>();

        NovaApi api = abc(new String[]{"beam-perfectsense-deepanjan@brightspot.com", "fdb709a02ea946f58f4c3d84013fc0bc"});

        api.getConfiguredRegions();

        Providers.all().forEach(o -> names.add(o.getName()));
        Apis.all().forEach(o -> names.add(o.getName()));

        try {
            AutoscaleApi autoscaleApi = createContextBuilder("rackspace-autoscale-us")
                .buildApi(AutoscaleApi.class);
        } catch (Exception ex) {
            throw ex;
        }

        System.out.println("\n Aloha got ya.");

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        return null;
    }
}
