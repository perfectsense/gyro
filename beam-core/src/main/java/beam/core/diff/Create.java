package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Create extends Change {

    private final Resource resource;

    public Create(Resource resource) {
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write("@|green + Create %s|@", resource.toDisplayString());
    }

    @Override
    protected void doExecute() {
        resource.create();
    }

}
