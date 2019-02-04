package beam.openstack;

import beam.lang.Resource;

public abstract class OpenstackResource extends Resource {
    @Override
    public Class resourceCredentialsClass() {
        return OpenstackCredentials.class;
    }
}
