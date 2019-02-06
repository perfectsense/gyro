package beam.core.diff;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Delete extends Change {

    private final Resource resource;

    public Delete(Resource resource) {
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write("@|red - Delete %s|@", resource.toDisplayString());
    }

    @Override
    protected void doExecute() {
        resource.delete();
    }

}
