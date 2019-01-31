package beam.plugins.ssh;

import beam.commands.AbstractConfigCommand;
import beam.core.BeamCore;
import beam.core.BeamInstance;
import beam.lang.Resource;
import beam.lang.FileBackend;
import beam.lang.ast.scope.FileScope;
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
    protected void doExecute(FileScope current, FileScope pending) throws Exception {

        BeamCore.ui().write("\n");

        List<BeamInstance> instances = new ArrayList<>();
        for (Resource resource : pending.getFileScope().getState().getFileScope().getResources().values()) {
            if (BeamInstance.class.isAssignableFrom(resource.getClass())) {
                BeamInstance instance = (BeamInstance) resource;

                instances.add(instance);

                if (refresh()) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...", resource.resourceType(), resource.resourceIdentifier());
                    resource.refresh();

                    FileBackend fileBackend = pending.getFileScope().getFileBackend();
                    fileBackend.save(pending.getFileScope().getState());
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
