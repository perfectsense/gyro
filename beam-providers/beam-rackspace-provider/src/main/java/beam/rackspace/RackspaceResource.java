package beam.rackspace;

import beam.lang.Resource;

public abstract class RackspaceResource extends Resource {
    @Override
    public Class resourceCredentialsClass() {
        return RackspaceCredentials.class;
    }
}
