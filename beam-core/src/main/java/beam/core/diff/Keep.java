package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Keep extends Change {

    private final Resource resource;

    public Keep(Resource resource) {
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write(resource.toDisplayString());
    }

    @Override
    protected void doExecute() {
    }

}
