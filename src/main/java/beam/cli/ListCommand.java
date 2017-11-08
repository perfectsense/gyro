package beam.cli;

import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.List;

import beam.BeamCloud;
import beam.BeamInstance;

@Command(name = "list", description = "List instances.")
public class ListCommand extends AbstractInstanceCommand {

    private static final Table LIST_TABLE = new Table().
            addColumn("Instance ID", 20).
            addColumn("Environment", 15).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65).
            addColumn("Date", 30);

    @Option(name = { "-r", "--refresh" }, description = "Refresh the instance data from the cloud provider.")
    public boolean refresh;

    @Override
    protected boolean isCacheOk() {
        return !refresh;
    }

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void first(BeamCloud cloud) throws Exception {
                LIST_TABLE.writeHeader(out);
                out.flush();
            }

            @Override
            public void each(BeamCloud cloud, BeamInstance instance) throws Exception {
                String environment = instance.getEnvironment();
                if (instance.isSandboxed()) {
                    environment = "@|blue sandbox (" + environment + ") |@";
                }

                LIST_TABLE.writeRow(
                        out,
                        instance.getId(),
                        environment,
                        instance.getLocation(),
                        instance.getLayer(),
                        instance.getState(),
                        instance.getHostname(),
                        instance.getDate());
                out.flush();
            }
            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                LIST_TABLE.writeFooter(out);
            }
        };
    }
}
