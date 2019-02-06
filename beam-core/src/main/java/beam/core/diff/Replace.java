package beam.core.diff;

import java.util.Set;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Replace extends Change {

    private final Resource currentResource;
    private final Resource pendingResource;
    private final Set<String> changedProperties;
    private final String changedDisplay;

    public Replace(Resource currentResource, Resource pendingResource, Set<String> changedProperties, String changedDisplay) {
        this.currentResource = currentResource;
        this.pendingResource = pendingResource;
        this.changedProperties = changedProperties;
        this.changedDisplay = changedDisplay;
    }

    @Override
    public Resource getResource() {
        return pendingResource;
    }

    @Override
    public void writeTo(BeamUI ui) {
        ui.write(
                "@|blue * Replace %s (%s)|@",
                currentResource.toDisplayString(),
                changedDisplay);
    }

    @Override
    protected void doExecute() {
    }

}
