package beam.core.diff;

import java.util.Set;

import beam.core.BeamUI;
import beam.lang.Resource;

public class Update extends Change {

    private final Resource currentResource;
    private final Resource pendingResource;
    private final Set<String> changedProperties;
    private final String changedDisplay;

    public Update(Resource currentResource, Resource pendingResource, Set<String> changedProperties, String changedDisplay) {
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
        String s = String.format(
                "Update %s (%s)",
                currentResource.toDisplayString(),
                changedDisplay);

        if (s.contains("@|")) {
            ui.write(" * %s", s);

        } else {
            ui.write("@|yellow * %s|@", s);
        }
    }

    @Override
    protected void doExecute() {
        pendingResource.update(currentResource, changedProperties);
    }

}
