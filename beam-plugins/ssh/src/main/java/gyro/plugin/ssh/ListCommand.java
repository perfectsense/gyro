package gyro.plugin.ssh;

import beam.core.BeamCore;
import beam.core.BeamInstance;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.airline.Command;

import java.util.List;

@Command(name = "list", description = "List instances found in provided config file.")
public class ListCommand extends AbstractInstanceCommand {

    private static final Table LIST_TABLE = new Table()
        .addColumn("Instance ID", 20)
        .addColumn("State", 12)
        .addColumn("Launch Date", 20)
        .addColumn("Hostname", 65);

    @Override
    public void doExecute(List<BeamInstance> instances) {
        LIST_TABLE.writeHeader(BeamCore.ui());

        for (BeamInstance instance : instances) {
            LIST_TABLE.writeRow(
                BeamCore.ui(),
                instance.getInstanceId(),
                instance.getState(),
                instance.getLaunchDate(),
                getHostname(instance)
            );
        }

        LIST_TABLE.writeFooter(BeamCore.ui());
    }

    public String getHostname(BeamInstance instance) {
        if (!ObjectUtils.isBlank(instance.getHostname())) {
            return instance.getHostname();
        }

        if (!ObjectUtils.isBlank(instance.getPublicIpAddress())) {
            return instance.getPublicIpAddress();
        }

        if (!ObjectUtils.isBlank(instance.getPrivateIpAddress())) {
            return instance.getPrivateIpAddress();
        }

        return "";
    }

}
