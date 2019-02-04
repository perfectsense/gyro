package beam.openstack;

import beam.core.BeamException;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;

import java.util.Properties;
import java.util.Set;

@ResourceName("test-resource")
public class OpenstackTestResource extends OpenstackResource {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private final String userName = "";
    private final String apiKey = "";

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public void create() {
        Boolean isFailed = false;

        System.out.println("\n Before testing provider 'rackspace-autoscale-us'.");

        try {
            AutoscaleApi autoscaleApi = createContextBuilder("rackspace-autoscale-us")
                .buildApi(AutoscaleApi.class);
        } catch (Exception ex) {
            System.out.println("\n Failed : " + ex.getMessage());
            isFailed = true;
        }

        System.out.println("\n After testing provider 'rackspace-autoscale-us'.");

        if (isFailed) {
            throw new BeamException("Failed!!!!");
        }

    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

    }

    @Override
    public void delete() {

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("test resource openstack ");

        return sb.toString();
    }



    private ContextBuilder createContextBuilder(String providerOrApi) {
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(providerOrApi)
            .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule()));

        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "20000");
        overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, "20000");
        contextBuilder.credentials(userName, apiKey);

        contextBuilder.overrides(overrides);

        return contextBuilder;
    }
}
