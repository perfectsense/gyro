package gyro.plugin.ssh;

import beam.commands.AbstractConfigCommand;
import beam.core.BeamCore;
import beam.core.BeamInstance;
import beam.lang.Resource;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;
import io.airlift.airline.Option;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractInstanceCommand extends AbstractConfigCommand {

    @Option(name = { "-r", "--refresh" }, description = "Refresh instance data from the cloud provider.")
    public boolean refresh;

    public boolean refresh() {
        return refresh;
    }

    public abstract void doExecute(List<BeamInstance> instances) throws Exception;

    @Override
    protected void doExecute(RootScope current, RootScope pending, State state) throws Exception {

        BeamCore.ui().write("\n");

        List<BeamInstance> instances = new ArrayList<>();
        for (Resource resource : current.findAllResources()) {
            if (BeamInstance.class.isAssignableFrom(resource.getClass())) {
                BeamInstance instance = (BeamInstance) resource;

                instances.add(instance);

                if (refresh()) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...", resource.resourceType(), resource.resourceIdentifier());
                    resource.refresh();

                    pending.getBackend().save(current);
                    BeamCore.ui().write("\n");
                }
            }
        }

        if (instances.isEmpty()) {
            BeamCore.ui().write("@|red No instances found.|@\n");
            return;
        }

        doExecute(instances);
    }

}
