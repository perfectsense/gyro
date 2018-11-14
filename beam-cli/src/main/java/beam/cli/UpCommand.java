package beam.cli;

import beam.core.BeamException;
import beam.core.extensions.ProviderExtension;
import beam.lang.BCL;
import beam.lang.BeamConfig;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;

import java.util.List;

@Command(name = "up", description = "Updates all resources to match the configuration.")
public class UpCommand extends AbstractCommand {

    @Arguments
    private List<String> arguments;

    @Override
    public void doExecute() throws Exception {

        if (getArguments().size() < 1) {
            throw new BeamException("Beam configuration file required.");
        }

        try {
            BCL.init();
            BCL.addExtension(new ProviderExtension());
            BeamConfig root = BCL.parse(getArguments().get(0));
            BCL.resolve(root);

            System.out.println(root);
        } finally {
            BCL.shutdown();
        }

    }

    public List<String> getArguments() {
        return arguments;
    }
}
