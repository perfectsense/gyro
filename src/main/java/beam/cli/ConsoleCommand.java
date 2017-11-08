package beam.cli;

import beam.BeamCloud;
import io.airlift.command.Command;
import io.airlift.command.Option;

@Command(name = "console", description = "Log into AWS web console.")
public class ConsoleCommand extends AbstractGlobalCommand {

    @Option(name = {"--readonly"}, description = "Read-only access.")
    public boolean readonly;

    @Option(name = {"--url"}, description = "Output login URL but don't open browser.")
    public boolean urlOnly;

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {
            @Override
            public void each(BeamCloud cloud) throws Exception {
                cloud.consoleLogin(readonly, urlOnly, out);
            }
        };
    }
}