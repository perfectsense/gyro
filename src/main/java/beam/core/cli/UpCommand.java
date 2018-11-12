package beam.core.cli;

import beam.core.BeamCommand;
import beam.lang.BCL;
import beam.lang.BeamConfig;
import io.airlift.airline.Command;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand implements BeamCommand {

    @Override
    public void execute() throws Exception {

        try {
            BCL.init();
            BeamConfig root = BCL.parse("./example.bcl");
            BCL.resolve(root);

            System.out.println(root);
        } finally {
            BCL.shutdown();
        }

    }

}
